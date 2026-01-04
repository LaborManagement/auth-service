package com.example.userauth.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userauth.dto.AuthorizationMatrix;
import com.example.userauth.dto.EndpointAuthorizationMetadata;
import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.PageAction;
import com.example.userauth.entity.UIPage;
import com.example.userauth.entity.User;
import com.example.userauth.repository.PageActionRepository;
import com.example.userauth.repository.UIPageRepository;
import com.example.userauth.repository.UserRepository;

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
     * Deduplicates endpoints since multiple page_actions may link to the same
     * endpoint
     *
     * @param pageId the UI page id
     * @return List of unique endpoint details for the page
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndpointsForPage(Long pageId) {
        List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(pageId);

        // Use LinkedHashMap to maintain insertion order and ensure uniqueness by
        // endpoint ID
        Map<Long, Map<String, Object>> uniqueEndpoints = new LinkedHashMap<>();

        for (PageAction action : actions) {
            if (action.getEndpoint() != null) {
                Long endpointId = action.getEndpoint().getId();

                // Only add if not already present (deduplicate by endpoint ID)
                if (!uniqueEndpoints.containsKey(endpointId)) {
                    uniqueEndpoints.put(endpointId, buildEndpointSummary(action.getEndpoint()));
                }
            }
        }

        return new ArrayList<>(uniqueEndpoints.values());
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    private final UserRepository userRepository;
    private final UIPageRepository uiPageRepository;
    private final PageActionRepository pageActionRepository;
    private final RoleAuthorizationService roleAuthorizationService;
    private final EndpointAuthorizationService endpointAuthorizationService;
    private final AuthorizationMatrixService authorizationMatrixService;

    public AuthorizationService(
            UserRepository userRepository,
            UIPageRepository uiPageRepository,
            PageActionRepository pageActionRepository,
            RoleAuthorizationService roleAuthorizationService,
            EndpointAuthorizationService endpointAuthorizationService,
            AuthorizationMatrixService authorizationMatrixService) {
        this.userRepository = userRepository;
        this.uiPageRepository = uiPageRepository;
        this.pageActionRepository = pageActionRepository;
        this.roleAuthorizationService = roleAuthorizationService;
        this.endpointAuthorizationService = endpointAuthorizationService;
        this.authorizationMatrixService = authorizationMatrixService;
    }

    /**
     * Get comprehensive authorization data for a user
     * 
     * @param userId The user ID
     * @return Map containing roles, policies, pages with actions, and accessible
     *         endpoints
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAuthorizations(Long userId) {
        logger.debug("Building authorization response for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        AuthorizationMatrix matrix = roleAuthorizationService.buildAuthorizationMatrix(user);

        logger.debug("User {} has roles: {}", userId, matrix.getRoles());

        // Get accessible endpoints based on user's roles and policies
        Set<Long> accessibleEndpointIds = getAccessibleEndpointIds(matrix.getRoles());

        List<Map<String, Object>> pages = getAccessiblePagesByEndpoints(matrix.getRoles(), accessibleEndpointIds);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", user.getUsername());
        response.put("user_type", user.getUserType());
        response.put("roles", matrix.getRoles());
        response.put("pages", pages);
        response.put("version", System.currentTimeMillis());

        logger.debug("Authorization response built successfully for user: {}", userId);
        return response;
    }

    /**
     * Build an authorization matrix for backend enforcement.
     * This reuses the same logic used for the UI payload.
     */
    @Transactional(readOnly = true)
    public AuthorizationMatrix buildAuthorizationMatrix(Long userId) {
        return roleAuthorizationService.buildAuthorizationMatrix(userId);
    }

    /**
     * Build the RBAC access matrix (user → role → policy → endpoint) for a specific
     * user.
     *
     * @param userId The user ID to evaluate
     * @return Map containing RBAC structures and summary metadata for the user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAccessMatrixForUser(Long userId) {
        return authorizationMatrixService.getUserAccessMatrixForUser(userId);
    }

    private Map<String, Object> buildEndpointSummary(Endpoint endpoint) {
        if (endpoint == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", endpoint.getId());
        summary.put("service", endpoint.getService());
        summary.put("version", endpoint.getVersion());
        summary.put("method", endpoint.getMethod());
        summary.put("path", endpoint.getPath());
        summary.put("module", endpoint.getModule());
        summary.put("description", endpoint.getDescription());
        return summary;
    }

    /**
     * Build the UI access matrix (page → action → endpoint) for administrative
     * review.
     *
     * @return Map containing UI structures and summary metadata
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUiAccessMatrixForPage(Long pageId) {
        return authorizationMatrixService.getUiAccessMatrixForPage(pageId);
    }

    /**
     * Resolve the policies that guard a specific endpoint definition.
     * Returns an empty set if the endpoint is not cataloged or has no policies.
     */
    @Transactional(readOnly = true)
    public EndpointAuthorizationMetadata getEndpointAuthorizationMetadata(String httpMethod, String requestPath) {
        return endpointAuthorizationService.getEndpointAuthorizationMetadata(httpMethod, requestPath);
    }

    /**
     * Get all endpoint IDs accessible to specific roles via their policies
     */
    private Set<Long> getAccessibleEndpointIds(Set<String> roleNames) {
        Set<Long> endpointIds = roleAuthorizationService.getAccessibleEndpointIds(roleNames);
        logger.debug("Found {} accessible endpoints for roles: {}", endpointIds.size(), roleNames);
        return endpointIds;
    }

    /**
     * Get accessible pages for user's roles based on endpoint access
     */
    private List<Map<String, Object>> getAccessiblePagesByEndpoints(Set<String> roleNames,
            Set<Long> accessibleEndpointIds) {
        List<UIPage> allPages = uiPageRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<Map<String, Object>> accessiblePages = new ArrayList<>();
        Set<Long> accessiblePageIds = new HashSet<>();

        // First pass: collect pages with actions that the user can perform
        for (UIPage page : allPages) {
            List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(page.getId());
            // Only include actions where the user has access to the linked endpoint
            List<Map<String, Object>> userActions = actions.stream()
                    .filter(action -> action.getEndpoint() != null
                            && accessibleEndpointIds.contains(action.getEndpoint().getId()))
                    .map(action -> {
                        Map<String, Object> actionData = new LinkedHashMap<>();
                        actionData.put("id", action.getId());
                        actionData.put("action", action.getAction());
                        actionData.put("label", action.getLabel());
                        actionData.put("displayOrder", action.getDisplayOrder());
                        actionData.put("endpoint", buildEndpointSummary(action.getEndpoint()));
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

        // Second pass: add parent pages that don't have actions but have accessible
        // children
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
        return authorizationMatrixService.getUserAccessMatrixForAllUsers();
    }

    /**
     * Build the UI access matrix hierarchy (page → action → endpoint) for all
     * active pages.
     *
     * @return Map containing UI structures and summary metadata for all pages
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUiAccessMatrixHierarchy() {
        return authorizationMatrixService.getUiAccessMatrixHierarchy();
    }
}
