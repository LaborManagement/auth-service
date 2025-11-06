package com.example.userauth.service;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.entity.PageAction;
import com.example.userauth.entity.Policy;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.RolePolicy;
import com.example.userauth.entity.UIPage;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRoleAssignment;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PageActionRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RolePolicyRepository;
import com.example.userauth.repository.UIPageRepository;
import com.example.userauth.repository.UserRepository;
import com.example.userauth.repository.UserRoleAssignmentRepository;
import com.example.userauth.service.dto.AuthorizationMatrix;
import com.example.userauth.service.dto.EndpointAuthorizationMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Authorization Service - Unified authorization flow implementation
 * 
 * ARCHITECTURE:
 * User → UserRoleAssignment → Role → Policy → Endpoint
 * PageAction → Endpoint (direct FK)
 * 
 * PRINCIPLE: Policy is the single source of truth
 * - Policy grants access to specific Endpoints
 * - User access is determined by: Role → Policy → Endpoints
 * 
 * Returns policies, endpoints, and UI pages for authenticated users
 */
@Service
public class AuthorizationService {
    /**
     * Get all endpoints for a given UI page id (regardless of user)
     * Deduplicates endpoints since multiple page_actions may link to the same endpoint
     *
     * @param pageId the UI page id
     * @return List of unique endpoint details for the page
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndpointsForPage(Long pageId) {
        List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(pageId);
        
        // Use LinkedHashMap to maintain insertion order and ensure uniqueness by endpoint ID
        Map<Long, Map<String, Object>> uniqueEndpoints = new LinkedHashMap<>();
        
        for (PageAction action : actions) {
            if (action.getEndpoint() != null) {
                Long endpointId = action.getEndpoint().getId();
                
                // Only add if not already present (deduplicate by endpoint ID)
                if (!uniqueEndpoints.containsKey(endpointId)) {
                    Map<String, Object> endpointData = new HashMap<>();
                    endpointData.put("id", endpointId);
                    endpointData.put("method", action.getEndpoint().getMethod());
                    endpointData.put("path", action.getEndpoint().getPath());
                    endpointData.put("service", action.getEndpoint().getService());
                    endpointData.put("version", action.getEndpoint().getVersion());
                    endpointData.put("description", action.getEndpoint().getDescription());
                    endpointData.put("ui_type", action.getEndpoint().getUiType());
                    uniqueEndpoints.put(endpointId, endpointData);
                }
            }
        }
        
        return new ArrayList<>(uniqueEndpoints.values());
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleRepository;
    private final PolicyRepository policyRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;
    private final UIPageRepository uiPageRepository;
    private final PageActionRepository pageActionRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final long ENDPOINT_CACHE_TTL_MS = 30_000L;

    private final Map<String, List<EndpointDescriptor>> endpointCache = new ConcurrentHashMap<>();
    private final AtomicLong endpointCacheLoadedAt = new AtomicLong(0);

    public AuthorizationService(
            UserRepository userRepository,
            UserRoleAssignmentRepository userRoleRepository,
            PolicyRepository policyRepository,
            EndpointRepository endpointRepository,
            EndpointPolicyRepository endpointPolicyRepository,
            UIPageRepository uiPageRepository,
            PageActionRepository pageActionRepository,
            RolePolicyRepository rolePolicyRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.policyRepository = policyRepository;
        this.endpointRepository = endpointRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
        this.uiPageRepository = uiPageRepository;
        this.pageActionRepository = pageActionRepository;
        this.rolePolicyRepository = rolePolicyRepository;
    }

    /**
     * Get comprehensive authorization data for a user
     * 
     * @param userId The user ID
     * @return Map containing roles, policies, pages with actions, and accessible endpoints
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAuthorizations(Long userId) {
        logger.debug("Building authorization response for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        AuthorizationMatrix matrix = buildAuthorizationMatrix(user);

        logger.debug("User {} has roles: {}", userId, matrix.getRoles());

        // Get accessible endpoints based on user's roles and policies
        Set<Long> accessibleEndpointIds = getAccessibleEndpointIds(matrix.getRoles());
        
        List<Map<String, Object>> pages = getAccessiblePagesByEndpoints(matrix.getRoles(), accessibleEndpointIds);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", user.getUsername());
        response.put("roles", matrix.getRoles());
        response.put("pages", pages);
        response.put("version", System.currentTimeMillis());

        logger.debug("Authorization response built successfully for user: {}", userId);
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

    /**
     * Build an authorization matrix for backend enforcement.
     * This reuses the same logic used for the UI payload.
     */
    @Transactional(readOnly = true)
    public AuthorizationMatrix buildAuthorizationMatrix(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return buildAuthorizationMatrix(user);
    }

    private AuthorizationMatrix buildAuthorizationMatrix(User user) {
        List<UserRoleAssignment> userRoles = userRoleRepository.findByUserId(user.getId());
        Set<String> roleNames = userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
        Set<String> policyNames = new HashSet<>();
        for (String roleName : roleNames) {
            policyNames.addAll(rolePolicyRepository.findPolicyNamesByRoleName(roleName));
        }
        return new AuthorizationMatrix(user.getId(), user.getPermissionVersion(), roleNames, policyNames);
    }

    /**
     * Build the RBAC access matrix (user → role → policy → endpoint) for a specific user.
     *
     * @param userId The user ID to evaluate
     * @return Map containing RBAC structures and summary metadata for the user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAccessMatrixForUser(Long userId) {
        logger.debug("Building user access matrix for administrative review. userId={}", userId);

        List<User> users = resolveUsers(userId);
        List<RolePolicy> activeRolePolicies = rolePolicyRepository.findByIsActiveTrue();
        List<Policy> activePolicies = policyRepository.findByIsActiveTrue();
        List<Endpoint> allEndpoints = endpointRepository.findAll();

        Map<Long, Policy> activePoliciesById = activePolicies.stream()
                .collect(Collectors.toMap(Policy::getId, Function.identity()));

        Map<Long, Endpoint> activeEndpointsById = allEndpoints.stream()
                .filter(endpoint -> Boolean.TRUE.equals(endpoint.getIsActive()))
                .collect(Collectors.toMap(Endpoint::getId, Function.identity()));

        Map<Long, List<RolePolicy>> rolePoliciesByRoleId = activeRolePolicies.stream()
                .filter(rolePolicy -> rolePolicy.getRole() != null && rolePolicy.getPolicy() != null)
                .collect(Collectors.groupingBy(rolePolicy -> rolePolicy.getRole().getId()));

        List<Long> activePolicyIds = new ArrayList<>(activePoliciesById.keySet());
        List<EndpointPolicy> endpointPolicies = activePolicyIds.isEmpty()
                ? Collections.emptyList()
                : endpointPolicyRepository.findByPolicyIdIn(activePolicyIds);

        Map<Long, Map<Long, Endpoint>> endpointsByPolicyId = new HashMap<>();
        Set<Long> allLinkedEndpointIds = new LinkedHashSet<>();
        for (EndpointPolicy endpointPolicy : endpointPolicies) {
            Policy policy = endpointPolicy.getPolicy();
            Endpoint endpoint = endpointPolicy.getEndpoint();
            if (policy == null || endpoint == null) {
                continue;
            }
            Endpoint activeEndpoint = activeEndpointsById.get(endpoint.getId());
            if (activeEndpoint == null) {
                continue;
            }
            allLinkedEndpointIds.add(activeEndpoint.getId());
            endpointsByPolicyId
                    .computeIfAbsent(policy.getId(), id -> new LinkedHashMap<>())
                    .put(activeEndpoint.getId(), activeEndpoint);
        }

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

    /**
     * Build the UI access matrix (page → action → endpoint) for administrative review.
     *
     * @return Map containing UI structures and summary metadata
     */
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

    /**
     * Resolve the policies that guard a specific endpoint definition.
     * Returns an empty set if the endpoint is not cataloged or has no policies.
     */
    @Transactional(readOnly = true)
    public EndpointAuthorizationMetadata getEndpointAuthorizationMetadata(String httpMethod, String requestPath) {
        String normalizedMethod = httpMethod != null ? httpMethod.toUpperCase(Locale.ROOT) : "GET";
        String normalizedPath = normalizePath(requestPath);

        Optional<EndpointDescriptor> endpointOpt = findMatchingEndpoint(normalizedMethod, normalizedPath);
        if (endpointOpt.isEmpty()) {
            logger.debug("No endpoint catalog match for method={} path={}", normalizedMethod, normalizedPath);
            return new EndpointAuthorizationMetadata(false, null, false, Set.of());
        }

        EndpointDescriptor endpoint = endpointOpt.get();
        if (!endpoint.active()) {
            logger.debug("Endpoint {} is inactive, denying by default", endpoint.id());
            return new EndpointAuthorizationMetadata(true, endpoint.id(), false, Set.of());
        }

        Set<Long> policyIds = endpointPolicyRepository.findByEndpointId(endpoint.id()).stream()
                .map(ep -> ep.getPolicy() != null ? ep.getPolicy().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (policyIds.isEmpty()) {
            logger.debug("Endpoint {} has no policies linked", endpoint.id());
            return new EndpointAuthorizationMetadata(true, endpoint.id(), false, Set.of());
        }

        return new EndpointAuthorizationMetadata(true, endpoint.id(), true, policyIds);
    }

    private Optional<EndpointDescriptor> findMatchingEndpoint(String method, String normalizedPath) {
        List<EndpointDescriptor> candidates = getEndpointsForMethod(method);
        for (EndpointDescriptor endpoint : candidates) {
            if (!endpoint.active()) {
                continue;
            }
            String endpointPath = normalizePath(endpoint.path());
            if (pathMatcher.match(endpointPath, normalizedPath)) {
                return Optional.of(endpoint);
            }
            for (String candidate : buildCompositePaths(endpoint, endpointPath)) {
                if (pathMatcher.match(candidate, normalizedPath)) {
                    return Optional.of(endpoint);
                }
            }
        }
        return Optional.empty();
    }

    private List<EndpointDescriptor> getEndpointsForMethod(String method) {
        long now = System.currentTimeMillis();
        if (now - endpointCacheLoadedAt.get() >= ENDPOINT_CACHE_TTL_MS) {
            endpointCache.clear();
            endpointCacheLoadedAt.set(now);
        }
        return endpointCache.computeIfAbsent(method, this::loadEndpointsForMethod);
    }

    private List<EndpointDescriptor> loadEndpointsForMethod(String method) {
        return endpointRepository.findByMethod(method).stream()
                .map(endpoint -> new EndpointDescriptor(
                        endpoint.getId(),
                        endpoint.getPath(),
                        endpoint.getService(),
                        endpoint.getVersion(),
                        Boolean.TRUE.equals(endpoint.getIsActive())))
                .collect(Collectors.toList());
    }

    private record EndpointDescriptor(Long id, String path, String service, String version, boolean active) {
    }

    private List<String> buildCompositePaths(EndpointDescriptor endpoint, String normalizedEndpointPath) {
        if (!StringUtils.hasText(endpoint.service())) {
            return List.of();
        }

        String serviceSegment = trimSlashes(endpoint.service());
        String versionSegment = trimSlashes(endpoint.version());

        String suffix = normalizedEndpointPath.startsWith("/")
                ? normalizedEndpointPath
                : "/" + normalizedEndpointPath;

        List<String> candidates = new ArrayList<>();

        // /api/{service}/{version}{path}
        StringBuilder builder = new StringBuilder("/api/").append(serviceSegment);
        if (StringUtils.hasText(versionSegment)) {
            builder.append("/").append(versionSegment);
        }
        candidates.add(mergePath(builder.toString(), suffix));

        // /api/{service}{path}
        candidates.add(mergePath("/api/" + serviceSegment, suffix));

        // Ensure unique values and drop any equal to the original path
        return candidates.stream()
                .filter(candidate -> !candidate.equals(normalizedEndpointPath))
                .distinct()
                .toList();
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String mergePath(String base, String suffix) {
        if (base.endsWith("/") && suffix.startsWith("/")) {
            return base.substring(0, base.length() - 1) + suffix;
        }
        if (!base.endsWith("/") && !suffix.startsWith("/")) {
            return base + "/" + suffix;
        }
        return base + suffix;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Get all endpoint IDs accessible to specific roles via their policies
     */
    private Set<Long> getAccessibleEndpointIds(Set<String> roleNames) {
        Set<Long> endpointIds = new HashSet<>();
        
        // Get all policies for the user's roles
        List<UserRoleAssignment> assignments = userRoleRepository.findAll().stream()
                .filter(ura -> roleNames.contains(ura.getRole().getName()))
                .collect(Collectors.toList());
        
        Set<Long> policyIds = assignments.stream()
                .flatMap(ura -> ura.getRole().getRolePolicies().stream())
                .map(rp -> rp.getPolicy().getId())
                .collect(Collectors.toSet());
        
        // Get all endpoints linked to these policies
        if (!policyIds.isEmpty()) {
            endpointIds = endpointPolicyRepository.findAll().stream()
                    .filter(ep -> policyIds.contains(ep.getPolicy().getId()))
                    .map(ep -> ep.getEndpoint().getId())
                    .collect(Collectors.toSet());
        }
        
        logger.debug("Found {} accessible endpoints for roles: {}", endpointIds.size(), roleNames);
        return endpointIds;
    }

    /**
     * Get accessible pages for user's roles based on endpoint access
     */
    private List<Map<String, Object>> getAccessiblePagesByEndpoints(Set<String> roleNames, Set<Long> accessibleEndpointIds) {
        List<UIPage> allPages = uiPageRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<Map<String, Object>> accessiblePages = new ArrayList<>();
        Set<Long> accessiblePageIds = new HashSet<>();

        // First pass: collect pages with actions that the user can perform
        for (UIPage page : allPages) {
            List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(page.getId());
            // Only include actions where the user has access to the linked endpoint
            List<Map<String, Object>> userActions = actions.stream()
                .filter(action -> action.getEndpoint() != null && accessibleEndpointIds.contains(action.getEndpoint().getId()))
                .map(action -> {
                    Map<String, Object> actionData = new HashMap<>();
                    actionData.put("name", action.getAction());
                    actionData.put("label", action.getLabel());
                    actionData.put("icon", action.getIcon());
                    actionData.put("variant", action.getVariant());
                    actionData.put("endpointId", action.getEndpoint().getId());
                    return actionData;
                })
                .collect(Collectors.toList());

            if (!userActions.isEmpty()) {
                Map<String, Object> pageData = new HashMap<>();
                pageData.put("id", page.getId());
                pageData.put("name", page.getLabel());
                pageData.put("path", page.getRoute());
                pageData.put("parentId", page.getParentId());
                pageData.put("icon", page.getIcon());
                pageData.put("displayOrder", page.getDisplayOrder());
                pageData.put("isMenuItem", page.getIsMenuItem());
                pageData.put("actions", userActions);

                accessiblePages.add(pageData);
                accessiblePageIds.add(page.getId());
            }
        }

        // Second pass: add parent pages that don't have actions but have accessible children
        Set<Long> parentIdsToAdd = new HashSet<>();
        for (Map<String, Object> pageData : accessiblePages) {
            Long parentId = (Long) pageData.get("parentId");
            if (parentId != null && !accessiblePageIds.contains(parentId)) {
                parentIdsToAdd.add(parentId);
            }
        }

        // Add parent pages
        for (Long parentId : parentIdsToAdd) {
            UIPage parentPage = allPages.stream()
                    .filter(p -> p.getId().equals(parentId))
                    .findFirst()
                    .orElse(null);
            
            if (parentPage != null) {
                Map<String, Object> parentData = new HashMap<>();
                parentData.put("id", parentPage.getId());
                parentData.put("name", parentPage.getLabel());
                parentData.put("path", parentPage.getRoute());
                parentData.put("parentId", parentPage.getParentId());
                parentData.put("icon", parentPage.getIcon());
                parentData.put("displayOrder", parentPage.getDisplayOrder());
                parentData.put("isMenuItem", parentPage.getIsMenuItem());
                parentData.put("actions", new ArrayList<>()); // No direct actions

                accessiblePages.add(parentData);
                accessiblePageIds.add(parentPage.getId());
            }
        }

        // Sort pages: parents first (by displayOrder), then children (by displayOrder)
        accessiblePages.sort((a, b) -> {
            Long parentIdA = (Long) a.get("parentId");
            Long parentIdB = (Long) b.get("parentId");
            Integer displayOrderA = (Integer) a.getOrDefault("displayOrder", 0);
            Integer displayOrderB = (Integer) b.getOrDefault("displayOrder", 0);
            
            // Both are root pages - sort by displayOrder
            if (parentIdA == null && parentIdB == null) {
                return Integer.compare(displayOrderA, displayOrderB);
            }
            
            // A is root, B is child - A comes first
            if (parentIdA == null && parentIdB != null) {
                return -1;
            }
            
            // A is child, B is root - B comes first
            if (parentIdA != null && parentIdB == null) {
                return 1;
            }
            
            // Both are children (neither is null at this point)
            // Check if same parent
            if (parentIdA != null && parentIdA.equals(parentIdB)) {
                return Integer.compare(displayOrderA, displayOrderB);
            }
            
            // Different parents - group by parent's display order
            Integer parentOrderA = accessiblePages.stream()
                    .filter(p -> p.get("id").equals(parentIdA))
                    .map(p -> (Integer) p.getOrDefault("displayOrder", 0))
                    .findFirst()
                    .orElse(0);
            Integer parentOrderB = accessiblePages.stream()
                    .filter(p -> p.get("id").equals(parentIdB))
                    .map(p -> (Integer) p.getOrDefault("displayOrder", 0))
                    .findFirst()
                    .orElse(0);
            
            return Integer.compare(parentOrderA, parentOrderB);
        });

        logger.debug("User has access to {} pages (including parent pages)", accessiblePages.size());
        return accessiblePages;
    }


    /**
     * Build the RBAC access matrix (user → role → policy → endpoint) for all users.
     *
     * @return Map containing RBAC structures and summary metadata for all users
     */
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
}
