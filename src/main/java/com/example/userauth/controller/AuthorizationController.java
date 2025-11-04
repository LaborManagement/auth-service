package com.example.userauth.controller;

import com.example.userauth.service.AuthorizationService;
import com.example.userauth.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.shared.common.annotation.Auditable;

/**
 * Controller for authorization and service catalog endpoints
 * Provides user authorizations and service metadata
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Authorization", description = "Authorization and Service Catalog APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final ServiceCatalogService serviceCatalogService;

    public AuthorizationController(
            AuthorizationService authorizationService,
            ServiceCatalogService serviceCatalogService) {
        this.authorizationService = authorizationService;
        this.serviceCatalogService = serviceCatalogService;
    }

    /**
     * Get comprehensive authorization data for the authenticated user
     * Returns: roles, permissions (can), pages, menus, endpoints
     * 
     * Frontend should call this after login and cache the response (using ETag)
     * Call again only when user permissions change or version number changes
     * 
     * @param authentication The authenticated user
     * @return Authorization data including roles, permissions (can), pages, and endpoints
     */
    @Auditable(action = "GET_USER_AUTHORIZATIONS", resourceType = "AUTHORIZATION")
    @GetMapping("/me/authorizations")
    @Operation(
            summary = "Get user authorizations",
            description = "Returns comprehensive authorization data including roles, permissions (can), accessible pages, menu tree, and endpoints for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
        public ResponseEntity<Map<String, Object>> getUserAuthorizations(Authentication authentication, HttpServletRequest request) {
                Long userId = extractUserIdFromAuthentication(authentication);
                Map<String, Object> authorizations = authorizationService.getUserAuthorizations(userId);
                        String etag = authorizations.get("version") != null ? String.valueOf(authorizations.get("version")) : ETagUtil.generateETag(authorizations.toString());
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (etag.equals(ifNoneMatch)) {
                                return ResponseEntity.status(304).eTag(etag).build();
                        }
                        return ResponseEntity.ok().eTag(etag).body(authorizations);
    }

    /**
     * Get service catalog metadata
     * Returns all available endpoints and pages (not user-specific)
     * Frontend can use this to discover available services
     * 
     * @return Service catalog with endpoints and pages
     */
    @Auditable(action = "GET_SERVICE_CATALOG", resourceType = "CATALOG")
    @GetMapping("/meta/service-catalog")
    @Operation(
            summary = "Get service catalog",
            description = "Returns metadata about all available endpoints and UI pages in the system",
            security = @SecurityRequirement(name = "bearerAuth")
    )
        public ResponseEntity<Map<String, Object>> getServiceCatalog(HttpServletRequest request) {
                Map<String, Object> catalog = serviceCatalogService.getServiceCatalog();
                String etag = catalog.get("version") != null ? String.valueOf(catalog.get("version")) : ETagUtil.generateETag(catalog.toString());
                String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                if (etag.equals(ifNoneMatch)) {
                        return ResponseEntity.status(304).eTag(etag).build();
                }
                return ResponseEntity.ok().eTag(etag).body(catalog);
    }

    /**
     * Get endpoints catalog grouped by module
     * 
     * @return Map of module -> endpoints
     */
    @Auditable(action = "GET_ENDPOINTS_CATALOG", resourceType = "CATALOG")
    @GetMapping("/meta/endpoints")
    @Operation(
            summary = "Get endpoints catalog",
            description = "Returns all available API endpoints grouped by module",
            security = @SecurityRequirement(name = "bearerAuth")
    )
                public ResponseEntity<Map<String, Object>> getEndpointsCatalog(@RequestParam(value = "page_id", required = false) Long pageId, HttpServletRequest request) {
                        Map<String, Object> response;
                        if (pageId != null) {
                                response = Map.of(
                                        "endpoints", authorizationService.getEndpointsForPage(pageId)
                                );
                        } else {
                                response = Map.of(
                                        "endpoints", serviceCatalogService.getEndpointsCatalog()
                                );
                        }
                        try {
                                String responseJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
                                String eTag = ETagUtil.generateETag(responseJson);
                                String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                                if (eTag.equals(ifNoneMatch)) {
                                        return ResponseEntity.status(304).eTag(eTag).build();
                                }
                                return ResponseEntity.ok().eTag(eTag).body(response);
                        } catch (Exception e) {
                                return ResponseEntity.internalServerError().build();
                        }
                }

    /**
     * Get pages catalog in hierarchical structure
     * 
     * @return Hierarchical list of UI pages
     */
    @Auditable(action = "GET_PAGES_CATALOG", resourceType = "CATALOG")
    @GetMapping("/meta/pages")
    @Operation(
            summary = "Get pages catalog",
            description = "Returns all available UI pages in hierarchical structure",
            security = @SecurityRequirement(name = "bearerAuth")
    )
        public ResponseEntity<Map<String, Object>> getPagesCatalog(HttpServletRequest request) {
                Map<String, Object> response = Map.of(
                        "pages", serviceCatalogService.getPagesCatalog()
                );
                try {
                        String responseJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                                return ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return ResponseEntity.ok().eTag(eTag).body(response);
                } catch (Exception e) {
                        return ResponseEntity.internalServerError().build();
                }
        }

    /**
     * Get RBAC access matrix for a specific user.
     *
     * @param userId User identifier
     * @return RBAC access matrix for review
     */
    @Auditable(action = "GET_USER_ACCESS_MATRIX", resourceType = "AUTHORIZATION")
    @GetMapping("/meta/user-access-matrix/{user_id}")
    @Operation(
            summary = "Get user access matrix",
            description = "Returns RBAC mappings (user → role → policy → endpoint) for a specific user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
        public ResponseEntity<Map<String, Object>> getUserAccessMatrix(
                @PathVariable("user_id") Long userId,
                HttpServletRequest request) {
                try {
                        Map<String, Object> matrix = authorizationService.getUserAccessMatrixForUser(userId);
                        String etag = matrix.get("version") != null ? String.valueOf(matrix.get("version")) : ETagUtil.generateETag(matrix.toString());
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (etag.equals(ifNoneMatch)) {
                                return ResponseEntity.status(304).eTag(etag).build();
                        }
                        return ResponseEntity.ok().eTag(etag).body(matrix);
                } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
                }
    }

    /**
     * Get UI access matrix (page → action → endpoint) for a specific page.
     *
     * @param pageId Page identifier
     * @return UI access matrix for review
     */
    @Auditable(action = "GET_UI_ACCESS_MATRIX", resourceType = "CATALOG")
    @GetMapping("/meta/ui-access-matrix/{page_id}")
    @Operation(
            summary = "Get UI access matrix",
            description = "Returns UI mappings (page → action → endpoint) for a specific page",
            security = @SecurityRequirement(name = "bearerAuth")
    )
        public ResponseEntity<Map<String, Object>> getUiAccessMatrix(
                @PathVariable("page_id") Long pageId,
                HttpServletRequest request) {
                try {
                        Map<String, Object> matrix = authorizationService.getUiAccessMatrixForPage(pageId);
                        String etag = matrix.get("version") != null ? String.valueOf(matrix.get("version")) : ETagUtil.generateETag(matrix.toString());
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (etag.equals(ifNoneMatch)) {
                                return ResponseEntity.status(304).eTag(etag).build();
                        }
                        return ResponseEntity.ok().eTag(etag).body(matrix);
                } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
                }
    }

    /**
     * Extract user ID from authentication object
     * This assumes your authentication principal contains user details
     * Adjust based on your actual authentication implementation
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        // If using UserDetails with User entity
        if (authentication.getPrincipal() instanceof com.example.userauth.entity.User) {
            com.example.userauth.entity.User user = 
                (com.example.userauth.entity.User) authentication.getPrincipal();
            return user.getId();
        }
        
        // If using username string, you need to look up the user
        // TODO: Inject UserRepository and implement username lookup
        // For now, returning a placeholder
        throw new IllegalStateException("Unable to extract user ID from authentication. Principal type: " + 
                authentication.getPrincipal().getClass().getName());
    }
}
