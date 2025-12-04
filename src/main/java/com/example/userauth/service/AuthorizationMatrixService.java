package com.example.userauth.service;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.PageAction;
import com.example.userauth.entity.Policy;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.RolePolicy;
import com.example.userauth.entity.UIPage;
import com.example.userauth.entity.User;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PageActionRepository;
import com.example.userauth.repository.RolePolicyRepository;
import com.example.userauth.repository.UIPageRepository;
import com.example.userauth.repository.UserRepository;
import com.example.userauth.service.support.PolicyEndpointMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds authorization and UI matrices for administrative and discovery endpoints.
 * Extracted from AuthorizationService to reduce surface area while keeping controller API unchanged.
 */
@Service
public class AuthorizationMatrixService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationMatrixService.class);

    private final UserRepository userRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final EndpointRepository endpointRepository;
    private final UIPageRepository uiPageRepository;
    private final PageActionRepository pageActionRepository;
    private final PolicyAuthorizationService policyAuthorizationService;

    public AuthorizationMatrixService(UserRepository userRepository,
                                      RolePolicyRepository rolePolicyRepository,
                                      EndpointRepository endpointRepository,
                                      UIPageRepository uiPageRepository,
                                      PageActionRepository pageActionRepository,
                                      PolicyAuthorizationService policyAuthorizationService) {
        this.userRepository = userRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        this.endpointRepository = endpointRepository;
        this.uiPageRepository = uiPageRepository;
        this.pageActionRepository = pageActionRepository;
        this.policyAuthorizationService = policyAuthorizationService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserAccessMatrixForUser(Long userId) {
        logger.debug("Building user access matrix for administrative review. userId={}", userId);

        List<User> users = resolveUsers(userId);
        List<RolePolicy> activeRolePolicies = rolePolicyRepository.findByIsActiveTrue();

        PolicyEndpointMapping mapping = policyAuthorizationService.buildPolicyEndpointMapping();
        Map<Long, Policy> activePoliciesById = mapping.activePoliciesById();
        Map<Long, Endpoint> activeEndpointsById = mapping.activeEndpointsById();

        Map<Long, List<RolePolicy>> rolePoliciesByRoleId = activeRolePolicies.stream()
                .filter(rolePolicy -> rolePolicy.getRole() != null && rolePolicy.getPolicy() != null)
                .collect(Collectors.groupingBy(rolePolicy -> rolePolicy.getRole().getId()));

        List<Long> activePolicyIds = new ArrayList<>(activePoliciesById.keySet());
        Map<Long, Map<Long, Endpoint>> endpointsByPolicyId = mapping.endpointsByPolicyId();
        Set<Long> allLinkedEndpointIds = mapping.allLinkedEndpointIds();

        Map<Long, List<PageAction>> actionsByEndpointId = loadActionsByEndpointIds(allLinkedEndpointIds);

        Set<Long> seenRoleIds = new LinkedHashSet<>();
        List<Role> roleEntities = new ArrayList<>();
        for (User user : users) {
            for (Role role : user.getRoles()) {
                if (role == null || role.getId() == null) {
                    continue;
                }
                if (seenRoleIds.add(role.getId())) {
                    roleEntities.add(role);
                }
            }
        }

        roleEntities.sort(Comparator.comparing(
                role -> role.getName() != null
                        ? role.getName().toLowerCase(Locale.ROOT)
                        : ""
        ));

        List<Map<String, Object>> roles = roleEntities.stream()
                .map(role -> buildRoleLinkage(role, rolePoliciesByRoleId, activePoliciesById,
                        endpointsByPolicyId, actionsByEndpointId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generated_at", Instant.now().toString());
        response.put("version", System.currentTimeMillis());
        response.put("filters", Map.of("user_id", userId));
        response.put("roles", roles);

        logger.debug("User access linkage built with {} roles and {} endpoints",
                roles.size(), allLinkedEndpointIds.size());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserAccessMatrixForAllUsers() {
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> userMatrices = new ArrayList<>();
        for (User user : allUsers) {
            try {
                Map<String, Object> matrix = getUserAccessMatrixForUser(user.getId());
                userMatrices.add(matrix);
            } catch (Exception ex) {
                // Optionally log or skip users that fail
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generated_at", Instant.now().toString());
        response.put("version", System.currentTimeMillis());
        response.put("users", userMatrices);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUiAccessMatrixForPage(Long pageId) {
        logger.debug("Building UI access matrix for administrative review. pageId={}", pageId);

        UIPage page = uiPageRepository.findById(pageId)
                .filter(uiPage -> Boolean.TRUE.equals(uiPage.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Page not found or inactive for id: " + pageId));

        List<PageAction> activeActions = pageActionRepository.findByPageIdAndIsActiveTrue(pageId);

        Set<Long> endpointIds = activeActions.stream()
                .map(PageAction::getEndpoint)
                .filter(Objects::nonNull)
                .map(Endpoint::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        Map<Long, Endpoint> endpointsById;
        if (endpointIds.isEmpty()) {
            endpointsById = Collections.emptyMap();
        } else {
            List<Endpoint> endpoints = endpointRepository.findByIdIn(new ArrayList<>(endpointIds));
            endpointsById = endpoints.stream()
                    .collect(Collectors.toMap(Endpoint::getId, Function.identity()));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generated_at", Instant.now().toString());
        response.put("version", System.currentTimeMillis());
        response.put("page_id", pageId);

        Map<String, Object> pageSummary = new LinkedHashMap<>();
        pageSummary.put("key", page.getKey());
        pageSummary.put("label", page.getLabel());
        pageSummary.put("route", page.getRoute());
        response.put("page", pageSummary);

        List<PageAction> sortedActions = new ArrayList<>(activeActions);
        sortedActions.sort(Comparator
                .comparing((PageAction action) -> action.getDisplayOrder() != null
                        ? action.getDisplayOrder()
                        : Integer.MAX_VALUE)
                .thenComparing(action -> action.getLabel() != null
                        ? action.getLabel().toLowerCase(Locale.ROOT)
                        : ""));

        Set<Long> seenActionIds = new HashSet<>();
        List<Map<String, Object>> actionDtos = new ArrayList<>();
        for (PageAction action : sortedActions) {
            if (action == null) {
                continue;
            }
            Long actionId = action.getId();
            if (actionId != null && !seenActionIds.add(actionId)) {
                continue;
            }
            Map<String, Object> actionMap = new LinkedHashMap<>();
            actionMap.put("label", action.getLabel());
            actionMap.put("action", action.getAction());

            Endpoint endpoint = action.getEndpoint();
            if (endpoint != null) {
                Endpoint resolved = endpointsById.getOrDefault(endpoint.getId(), endpoint);
                Map<String, Object> endpointSummary = new LinkedHashMap<>();
                endpointSummary.put("service", resolved.getService());
                endpointSummary.put("version", resolved.getVersion());
                endpointSummary.put("method", resolved.getMethod());
                endpointSummary.put("path", resolved.getPath());
                actionMap.put("endpoint", endpointSummary);
            }
            actionDtos.add(actionMap);
        }
        response.put("actions", actionDtos);

        logger.debug("UI access matrix built for page {} with {} actions, {} endpoints",
                pageId, actionDtos.size(), endpointIds.size());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUiAccessMatrixHierarchy() {
        List<UIPage> activePages = uiPageRepository.findByIsActiveTrue();
        List<Map<String, Object>> pageMatrices = new ArrayList<>();
        for (UIPage page : activePages) {
            try {
                Map<String, Object> matrix = getUiAccessMatrixForPage(page.getId());
                pageMatrices.add(matrix);
            } catch (Exception ex) {
                // Optionally log or skip pages that fail
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generated_at", Instant.now().toString());
        response.put("version", System.currentTimeMillis());
        response.put("pages", pageMatrices);
        return response;
    }

    private List<User> resolveUsers(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return userRepository.findById(userId)
                .map(List::of)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + userId));
    }

    private Map<Long, List<PageAction>> loadActionsByEndpointIds(Set<Long> endpointIds) {
        if (endpointIds == null || endpointIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PageAction> linkedActions = pageActionRepository
                .findByEndpointIdInAndIsActiveTrue(new ArrayList<>(endpointIds));
        if (linkedActions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<PageAction>> grouped = new LinkedHashMap<>();
        for (PageAction action : linkedActions) {
            if (action == null || action.getEndpoint() == null || action.getEndpoint().getId() == null) {
                continue;
            }
            grouped
                    .computeIfAbsent(action.getEndpoint().getId(), id -> new ArrayList<>())
                    .add(action);
        }
        return grouped;
    }

    private Map<String, Object> buildRoleLinkage(
            Role role,
            Map<Long, List<RolePolicy>> rolePoliciesByRoleId,
            Map<Long, Policy> policiesById,
            Map<Long, Map<Long, Endpoint>> endpointsByPolicyId,
            Map<Long, List<PageAction>> actionsByEndpointId) {

        List<RolePolicy> assignments = rolePoliciesByRoleId.getOrDefault(role.getId(), Collections.emptyList());
        Set<Long> seenPolicyIds = new LinkedHashSet<>();
        List<Map<String, Object>> policyDtos = new ArrayList<>();

        for (RolePolicy assignment : assignments) {
            Policy rawPolicy = assignment.getPolicy();
            if (rawPolicy == null || rawPolicy.getId() == null) {
                continue;
            }
            Policy policy = policiesById.get(rawPolicy.getId());
            if (policy == null || !seenPolicyIds.add(policy.getId())) {
                continue;
            }
            Map<String, Object> policyDto = buildPolicyLinkage(
                    policy,
                    endpointsByPolicyId.getOrDefault(policy.getId(), Collections.emptyMap()),
                    actionsByEndpointId);
            if (policyDto != null) {
                policyDtos.add(policyDto);
            }
        }

        if (policyDtos.isEmpty()) {
            return null;
        }

        policyDtos.sort(Comparator.comparing(
                policy -> ((String) policy.getOrDefault("name", "")).toLowerCase(Locale.ROOT)
        ));

        Map<String, Object> roleMap = new LinkedHashMap<>();
        roleMap.put("name", role.getName());
        roleMap.put("description", role.getDescription());
        roleMap.put("policies", policyDtos);
        return roleMap;
    }

    private Map<String, Object> buildPolicyLinkage(
            Policy policy,
            Map<Long, Endpoint> endpointsForPolicy,
            Map<Long, List<PageAction>> actionsByEndpointId) {

        List<Map<String, Object>> endpointDtos = new ArrayList<>();
        if (endpointsForPolicy != null) {
            endpointsForPolicy.values().stream()
                    .sorted(Comparator
                            .comparing((Endpoint endpoint) -> endpoint.getService() != null
                                    ? endpoint.getService().toLowerCase(Locale.ROOT)
                                    : "")
                            .thenComparing(endpoint -> endpoint.getPath() != null ? endpoint.getPath() : "")
                            .thenComparing(endpoint -> endpoint.getMethod() != null
                                    ? endpoint.getMethod().toLowerCase(Locale.ROOT)
                                    : ""))
                    .forEach(endpoint -> {
                        List<PageAction> actions = actionsByEndpointId.getOrDefault(endpoint.getId(), Collections.emptyList());
                        endpointDtos.add(buildEndpointLinkage(endpoint, actions));
                    });
        }

        if (endpointDtos.isEmpty()) {
            return null;
        }

        Map<String, Object> policyMap = new LinkedHashMap<>();
        policyMap.put("name", policy.getName());
        policyMap.put("description", policy.getDescription());
        policyMap.put("endpoints", endpointDtos);
        return policyMap;
    }

    private Map<String, Object> buildEndpointLinkage(Endpoint endpoint, List<PageAction> actions) {
        Map<String, Object> endpointMap = new LinkedHashMap<>();
        endpointMap.put("service", endpoint.getService());
        endpointMap.put("version", endpoint.getVersion());
        endpointMap.put("method", endpoint.getMethod());
        endpointMap.put("path", endpoint.getPath());

        List<PageAction> safeActions = actions == null
                ? Collections.emptyList()
                : new ArrayList<>(actions);
        safeActions.sort(Comparator
                .comparing((PageAction action) -> action.getDisplayOrder() != null
                        ? action.getDisplayOrder()
                        : Integer.MAX_VALUE)
                .thenComparing(action -> action.getLabel() != null
                        ? action.getLabel().toLowerCase(Locale.ROOT)
                        : ""));

        Set<Long> seenActionIds = new LinkedHashSet<>();
        List<Map<String, Object>> actionDtos = new ArrayList<>();
        for (PageAction action : safeActions) {
            if (action == null) {
                continue;
            }
            Long actionId = action.getId();
            if (actionId != null && !seenActionIds.add(actionId)) {
                continue;
            }
            UIPage page = action.getPage();
            Map<String, Object> actionMap = new LinkedHashMap<>();
            actionMap.put("action", action.getAction());
            actionMap.put("label", action.getLabel());
            if (page != null) {
                Map<String, Object> pageMap = new LinkedHashMap<>();
                pageMap.put("key", page.getKey());
                pageMap.put("label", page.getLabel());
                pageMap.put("route", page.getRoute());
                actionMap.put("page", pageMap);
            }
            actionDtos.add(actionMap);
        }

        endpointMap.put("page_actions", actionDtos);
        return endpointMap;
    }
}
