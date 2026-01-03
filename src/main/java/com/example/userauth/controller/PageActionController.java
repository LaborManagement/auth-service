package com.example.userauth.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.PageAction;
import com.example.userauth.entity.UIPage;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PageActionRepository;
import com.example.userauth.repository.UIPageRepository;
import com.shared.common.annotation.Auditable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin controller for managing page actions
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/auth-service/api/admin/page-actions")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Page Action Management", description = "Admin APIs for managing UI page actions.")
public class PageActionController {

    private final PageActionRepository pageActionRepository;
    private final UIPageRepository uiPageRepository;
    private final EndpointRepository endpointRepository;

    public PageActionController(
            PageActionRepository pageActionRepository,
            UIPageRepository uiPageRepository,
            EndpointRepository endpointRepository) {
        this.pageActionRepository = pageActionRepository;
        this.uiPageRepository = uiPageRepository;
        this.endpointRepository = endpointRepository;
    }

    /**
     * Get all page actions
     */
    @Auditable(action = "GET_ALL_PAGE_ACTIONS", resourceType = "PAGE_ACTION")
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all page actions", description = "Returns all page actions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page actions retrieved successfully")
    })
    public ResponseEntity<List<Map<String, Object>>> getAllPageActions() {
        List<PageAction> actions = pageActionRepository.findAll();
        List<Map<String, Object>> response = actions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get page action by ID
     */
    @Auditable(action = "GET_PAGE_ACTION_BY_ID", resourceType = "PAGE_ACTION")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get page action by ID", description = "Returns a page action by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page action found and returned"),
            @ApiResponse(responseCode = "404", description = "Page action not found")
    })
    public ResponseEntity<Map<String, Object>> getPageActionById(@PathVariable Long id) {
        return pageActionRepository.findById(id)
                .map(action -> ResponseEntity.ok(convertToResponse(action)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get actions for a specific page
     */
    @Auditable(action = "GET_ACTIONS_FOR_PAGE", resourceType = "PAGE_ACTION")
    @GetMapping("/page/{pageId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get actions for a specific page", description = "Returns all actions for a given page ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actions for page retrieved successfully")
    })
    public ResponseEntity<List<Map<String, Object>>> getActionsForPage(@PathVariable Long pageId) {
        List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(pageId);
        List<Map<String, Object>> response = actions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Create new page action
     */
    @Auditable(action = "CREATE_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @PostMapping
    @Transactional
    @Operation(summary = "Create new page action", description = "Creates a new page action.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page action created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or page/endpoint not found")
    })
    public ResponseEntity<Map<String, Object>> createPageAction(@RequestBody PageActionRequest request) {
        // Validate page
        UIPage page = uiPageRepository.findById(request.getPageId())
                .orElseThrow(() -> new RuntimeException("Page not found: " + request.getPageId()));

        // Validate endpoint if provided
        Endpoint endpoint = null;
        if (request.getEndpointId() != null) {
            endpoint = endpointRepository.findById(request.getEndpointId())
                    .orElseThrow(() -> new RuntimeException("Endpoint not found: " + request.getEndpointId()));
        }

        PageAction action = new PageAction();
        action.setPage(page);
        action.setEndpoint(endpoint);
        action.setLabel(request.getLabel());
        action.setAction(request.getAction());
        action.setIcon(request.getIcon() != null ? request.getIcon() : "");
        action.setVariant(request.getVariant() != null ? request.getVariant() : "default");
        action.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        action.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        PageAction saved = pageActionRepository.save(action);
        return ResponseEntity.ok(convertToResponse(saved));
    }

    /**
     * Update page action
     */
    @Auditable(action = "UPDATE_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update page action", description = "Updates an existing page action by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page action updated successfully"),
            @ApiResponse(responseCode = "404", description = "Page action not found")
    })
    public ResponseEntity<Map<String, Object>> updatePageAction(
            @PathVariable Long id,
            @RequestBody PageActionRequest request) {

        return pageActionRepository.findById(id)
                .map(action -> {
                    // Validate page if changed
                    if (request.getPageId() != null && !request.getPageId().equals(action.getPage().getId())) {
                        UIPage page = uiPageRepository.findById(request.getPageId())
                                .orElseThrow(() -> new RuntimeException("Page not found: " + request.getPageId()));
                        action.setPage(page);
                    }

                    // Validate endpoint if changed
                    if (request.getEndpointId() != null) {
                        Endpoint endpoint = endpointRepository.findById(request.getEndpointId())
                                .orElseThrow(
                                        () -> new RuntimeException("Endpoint not found: " + request.getEndpointId()));
                        action.setEndpoint(endpoint);
                    } else {
                        action.setEndpoint(null);
                    }

                    action.setLabel(request.getLabel());
                    action.setAction(request.getAction());
                    action.setIcon(request.getIcon() != null ? request.getIcon() : action.getIcon());
                    action.setVariant(request.getVariant() != null ? request.getVariant() : action.getVariant());
                    action.setDisplayOrder(
                            request.getDisplayOrder() != null ? request.getDisplayOrder() : action.getDisplayOrder());
                    action.setIsActive(request.getIsActive() != null ? request.getIsActive() : action.getIsActive());

                    PageAction updated = pageActionRepository.save(action);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete page action
     */
    @Auditable(action = "DELETE_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Delete page action", description = "Deletes a page action by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Page action deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Page action not found")
    })
    public ResponseEntity<Void> deletePageAction(@PathVariable Long id) {
        if (pageActionRepository.existsById(id)) {
            pageActionRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle page action active status
     */
    @Auditable(action = "TOGGLE_PAGE_ACTION_ACTIVE", resourceType = "PAGE_ACTION")
    @PatchMapping("/{id}/toggle-active")
    @Transactional
    @Operation(summary = "Toggle page action active status", description = "Toggles the active status of a page action by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page action status toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Page action not found")
    })
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        return pageActionRepository.findById(id)
                .map(action -> {
                    action.setIsActive(!action.getIsActive());
                    PageAction updated = pageActionRepository.save(action);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reorder page action
     */
    @Auditable(action = "REORDER_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @PatchMapping("/{id}/reorder")
    @Transactional
    @Operation(summary = "Reorder page action", description = "Updates the display order of a page action.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page action reordered successfully"),
            @ApiResponse(responseCode = "404", description = "Page action not found")
    })
    public ResponseEntity<Map<String, Object>> reorderPageAction(
            @PathVariable Long id,
            @RequestBody ReorderRequest request) {

        return pageActionRepository.findById(id)
                .map(action -> {
                    action.setDisplayOrder(request.getNewDisplayOrder());
                    PageAction updated = pageActionRepository.save(action);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Helper methods

    private Map<String, Object> convertToResponse(PageAction action) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", action.getId());
        response.put("label", action.getLabel());
        response.put("action", action.getAction());
        response.put("icon", action.getIcon());
        response.put("variant", action.getVariant());
        response.put("displayOrder", action.getDisplayOrder());
        response.put("isActive", action.getIsActive());
        response.put("createdAt", action.getCreatedAt());
        response.put("updatedAt", action.getUpdatedAt());

        // Page info
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("id", action.getPage().getId());
        pageInfo.put("label", action.getPage().getLabel());
        pageInfo.put("route", action.getPage().getRoute());
        response.put("page", pageInfo);

        // Endpoint info (if exists)
        if (action.getEndpoint() != null) {
            Map<String, Object> endpointInfo = new HashMap<>();
            endpointInfo.put("id", action.getEndpoint().getId());
            endpointInfo.put("method", action.getEndpoint().getMethod());
            endpointInfo.put("path", action.getEndpoint().getPath());
            response.put("endpoint", endpointInfo);
        } else {
            response.put("endpoint", null);
        }

        return response;
    }

    // DTO classes

    public static class PageActionRequest {
        private Long pageId;
        private Long endpointId;
        private String label;
        private String action;
        private String icon;
        private String variant;
        private Integer displayOrder;
        private Boolean isActive;

        // Getters and Setters
        public Long getPageId() {
            return pageId;
        }

        public void setPageId(Long pageId) {
            this.pageId = pageId;
        }

        public Long getEndpointId() {
            return endpointId;
        }

        public void setEndpointId(Long endpointId) {
            this.endpointId = endpointId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getVariant() {
            return variant;
        }

        public void setVariant(String variant) {
            this.variant = variant;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }

    public static class ReorderRequest {
        private Integer newDisplayOrder;

        public Integer getNewDisplayOrder() {
            return newDisplayOrder;
        }

        public void setNewDisplayOrder(Integer newDisplayOrder) {
            this.newDisplayOrder = newDisplayOrder;
        }
    }
}
