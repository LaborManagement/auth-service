package com.example.userauth.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.entity.Policy;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shared.common.annotation.Auditable;
import com.shared.common.util.ETagUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Admin controller for managing endpoints and their policy assignments
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Endpoint Management", description = "Admin APIs for managing endpoints and their policy assignments.")
public class EndpointController {

    private static final Logger logger = LoggerFactory.getLogger(EndpointController.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final EndpointRepository endpointRepository;
    private final PolicyRepository policyRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public EndpointController(
            EndpointRepository endpointRepository,
            PolicyRepository policyRepository,
            EndpointPolicyRepository endpointPolicyRepository) {
        this.endpointRepository = endpointRepository;
        this.policyRepository = policyRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

    /**
     * Get all endpoints with their policies
     */
    @Auditable(action = "GET_ALL_ENDPOINTS", resourceType = "ENDPOINT")
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all endpoints", description = "Returns all endpoints with their policies.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoints retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Map<String, Object>>> getAllEndpoints(HttpServletRequest request) {
        List<Endpoint> endpoints = endpointRepository.findAll();
        List<Map<String, Object>> response = endpoints.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(response);
        } catch (Exception e) {
            logger.error("Error processing endpoints response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get endpoint by ID with policies
     */
    @Auditable(action = "GET_ENDPOINT_BY_ID", resourceType = "ENDPOINT")
    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Operation(summary = "Get endpoint by ID", description = "Returns an endpoint by its ID with policies.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoint found and returned"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getEndpointById(@PathVariable(value = "id", required = true) Long id,
            HttpServletRequest request) {
        return endpointRepository.findByIdWithPolicies(id)
                .map(endpoint -> {
                    Map<String, Object> response = convertToResponse(endpoint);
                    try {
                        String responseJson = objectMapper.writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.status(304)
                                    .eTag(eTag).build();
                        }
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag)
                                .body(response);
                    } catch (Exception e) {
                        logger.error("Error processing endpoint response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity
                                .internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new endpoint
     */
    @PostMapping
    @Transactional
    @Auditable(action = "CREATE_ENDPOINT", resourceType = "ENDPOINT")
    @Operation(summary = "Create new endpoint", description = "Creates a new endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoint created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Map<String, Object>> createEndpoint(@RequestBody EndpointRequest request) {
        Endpoint endpoint = new Endpoint(
                request.getService(),
                request.getVersion(),
                request.getMethod(),
                request.getPath(),
                request.getDescription());
        endpoint.setIsActive(request.getIsActive());
        Endpoint saved = endpointRepository.save(endpoint);

        // Policy assignment via this request is deprecated/removed
        // assignPolicies(saved.getId(), ...); // No longer handled here

        // Fetch the endpoint with policies eagerly loaded
        Endpoint endpointWithPolicies = endpointRepository.findByIdWithPolicies(saved.getId())
                .orElseThrow(() -> new RuntimeException("Endpoint not found after creation"));

        return ResponseEntity.ok(convertToResponse(endpointWithPolicies));
    }

    /**
     * Update endpoint
     */
    @PutMapping("/{id}")
    @Transactional
    @Auditable(action = "UPDATE_ENDPOINT", resourceType = "ENDPOINT")
    @Operation(summary = "Update endpoint", description = "Updates an existing endpoint by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoint updated successfully"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    public ResponseEntity<Map<String, Object>> updateEndpoint(
            @PathVariable(value = "id", required = true) Long id,
            @RequestBody EndpointRequest request) {

        return endpointRepository.findById(id)
                .map(endpoint -> {
                    endpoint.setService(request.getService());
                    endpoint.setVersion(request.getVersion());
                    endpoint.setMethod(request.getMethod());
                    endpoint.setPath(request.getPath());
                    endpoint.setDescription(request.getDescription());
                    endpoint.setIsActive(request.getIsActive());
                    endpointRepository.save(endpoint);

                    // Policy assignment via this request is deprecated/removed
                    // endpointPolicyRepository.deleteByEndpointId(id);
                    // assignPolicies(id, ...); // No longer handled here

                    // Fetch the endpoint with policies eagerly loaded
                    Endpoint endpointWithPolicies = endpointRepository.findByIdWithPolicies(id)
                            .orElseThrow(() -> new RuntimeException("Endpoint not found"));

                    return ResponseEntity.ok(convertToResponse(endpointWithPolicies));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete endpoint
     */
    @DeleteMapping("/{id}")
    @Transactional
    @Auditable(action = "DELETE_ENDPOINT", resourceType = "ENDPOINT")
    @Operation(summary = "Delete endpoint", description = "Deletes an endpoint by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Endpoint deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    public ResponseEntity<Void> deleteEndpoint(@PathVariable(value = "id", required = true) Long id) {
        if (endpointRepository.existsById(id)) {
            // Delete endpoint policies first
            endpointPolicyRepository.deleteByEndpointId(id);
            // Delete endpoint
            endpointRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle endpoint active status
     */
    @PatchMapping("/{id}/toggle-active")
    @Auditable(action = "TOGGLE_ENDPOINT_ACTIVE", resourceType = "ENDPOINT")
    @Operation(summary = "Toggle endpoint active status", description = "Toggles the active status of an endpoint by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoint status toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable(value = "id", required = true) Long id) {
        return endpointRepository.findById(id)
                .map(endpoint -> {
                    endpoint.setIsActive(!endpoint.getIsActive());
                    Endpoint updated = endpointRepository.save(endpoint);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get policies assigned to an endpoint
     */
    @GetMapping("/{id}/policies")
    @Transactional(readOnly = true)
    @Operation(summary = "Get policies assigned to an endpoint", description = "Returns all policies assigned to an endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policies retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PolicySummary>> getEndpointPolicies(@PathVariable(value = "id", required = true) Long id,
            HttpServletRequest request) {
        List<EndpointPolicy> endpointPolicies = endpointPolicyRepository.findByEndpointId(id);
        List<PolicySummary> policies = endpointPolicies.stream()
                .map(EndpointPolicy::getPolicy)
                .map(PolicySummary::from)
                .collect(Collectors.toList());
        try {
            String responseJson = objectMapper.writeValueAsString(policies);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(policies);
        } catch (Exception e) {
            logger.error("Error processing policies response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Assign policies to endpoint
     */
    @PostMapping("/{id}/policies")
    @Transactional
    @Auditable(action = "ASSIGN_POLICIES_TO_ENDPOINT", resourceType = "ENDPOINT")
    @Operation(summary = "Assign policies to endpoint", description = "Assigns policies to an endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policies assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    public ResponseEntity<Map<String, Object>> assignPoliciesToEndpoint(
            @PathVariable(value = "id", required = true) Long id,
            @RequestBody PolicyAssignmentRequest request) {

        if (!endpointRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        assignPolicies(id, request.getPolicyIds());

        // Fetch the endpoint with policies eagerly loaded
        Endpoint endpointWithPolicies = endpointRepository.findByIdWithPolicies(id)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));

        return ResponseEntity.ok(convertToResponse(endpointWithPolicies));
    }

    /**
     * Remove policy from endpoint
     */
    @DeleteMapping("/{id}/policies/{policyId}")
    @Transactional
    @Auditable(action = "REMOVE_POLICY_FROM_ENDPOINT", resourceType = "ENDPOINT")
    @Operation(summary = "Remove policy from endpoint", description = "Removes a policy from an endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Policy removed from endpoint successfully")
    })
    public ResponseEntity<Void> removePolicyFromEndpoint(
            @PathVariable(value = "id", required = true) Long id,
            @PathVariable(value = "policyId", required = true) Long policyId) {

        endpointPolicyRepository.deleteByEndpointIdAndPolicyId(id, policyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk assign a single policy to multiple endpoints
     */
    @PostMapping("/bulk-policy-assignment")
    @Transactional
    @Auditable(action = "BULK_ASSIGN_POLICY_TO_ENDPOINTS", resourceType = "ENDPOINT")
    @Operation(summary = "Bulk assign a single policy to multiple endpoints", description = "Assigns a single policy to multiple endpoints in bulk.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy assigned to endpoints successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Map<String, Object>> bulkAssignPolicyToEndpoints(
            @RequestBody BulkPolicyAssignmentRequest request) {

        if (request.getPolicyId() == null) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (request.getEndpointIds() == null || request.getEndpointIds().isEmpty()) {
            throw new IllegalArgumentException("At least one endpointId is required");
        }

        Long policyId = request.getPolicyId();
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        long nextId = endpointPolicyRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);

        List<Long> newlyAssigned = new ArrayList<>();

        for (Long endpointId : request.getEndpointIds()) {
            Endpoint endpoint = endpointRepository.findById(endpointId)
                    .orElseThrow(() -> new IllegalArgumentException("Endpoint not found: " + endpointId));

            if (!endpointPolicyRepository.existsByEndpointIdAndPolicyId(endpointId, policyId)) {
                EndpointPolicy ep = new EndpointPolicy(endpoint, policy);
                ep.setId(nextId++);
                endpointPolicyRepository.save(ep);
                newlyAssigned.add(endpointId);
            }
        }

        List<Map<String, Object>> endpointSummaries = endpointPolicyRepository.findByPolicyId(policyId)
                .stream()
                .map(ep -> {
                    Endpoint endpoint = ep.getEndpoint();
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("id", endpoint.getId());
                    summary.put("service", endpoint.getService());
                    summary.put("version", endpoint.getVersion());
                    summary.put("method", endpoint.getMethod());
                    summary.put("path", endpoint.getPath());
                    summary.put("description", endpoint.getDescription());
                    return summary;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("policyId", policyId);
        response.put("newlyAssignedEndpointIds", newlyAssigned);
        response.put("totalEndpointCount", endpointSummaries.size());
        response.put("endpoints", endpointSummaries);

        return ResponseEntity.ok(response);
    }

    // Helper methods

    private void assignPolicies(Long endpointId, Set<Long> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return;
        }
        Endpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));

        long nextId = endpointPolicyRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);

        for (Long policyId : policyIds) {
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));

            // Check if already exists
            if (!endpointPolicyRepository.existsByEndpointIdAndPolicyId(endpointId, policyId)) {
                EndpointPolicy ep = new EndpointPolicy(endpoint, policy);
                ep.setId(nextId++);
                endpointPolicyRepository.save(ep);
            }
        }
    }

    private Map<String, Object> convertToResponse(Endpoint endpoint) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", endpoint.getId());
        response.put("service", endpoint.getService());
        response.put("version", endpoint.getVersion());
        response.put("method", endpoint.getMethod());
        response.put("path", endpoint.getPath());
        response.put("description", endpoint.getDescription());
        response.put("isActive", endpoint.getIsActive());
        response.put("createdAt", endpoint.getCreatedAt());
        response.put("updatedAt", endpoint.getUpdatedAt());

        // Add policies
        Set<EndpointPolicy> endpointPolicies = endpoint.getEndpointPolicies();
        List<Map<String, Object>> policies = endpointPolicies.stream()
                .map(ep -> {
                    Map<String, Object> pol = new HashMap<>();
                    pol.put("id", ep.getPolicy().getId());
                    pol.put("name", ep.getPolicy().getName());
                    pol.put("description", ep.getPolicy().getDescription());
                    return pol;
                })
                .collect(Collectors.toList());
        response.put("policies", policies);

        return response;
    }

    // DTO classes

    public static class EndpointRequest {
        private String service;
        private String version;
        private String method;
        private String path;
        private String description;
        private Boolean isActive = true;

        // Getters and Setters
        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
        // Removed policyIds
    }

    public static class PolicyAssignmentRequest {
        private Set<Long> policyIds;

        public Set<Long> getPolicyIds() {
            return policyIds;
        }

        public void setPolicyIds(Set<Long> policyIds) {
            this.policyIds = policyIds;
        }
    }

    public static class BulkPolicyAssignmentRequest {
        private Long policyId;
        private Set<Long> endpointIds;

        public Long getPolicyId() {
            return policyId;
        }

        public void setPolicyId(Long policyId) {
            this.policyId = policyId;
        }

        public Set<Long> getEndpointIds() {
            return endpointIds;
        }

        public void setEndpointIds(Set<Long> endpointIds) {
            this.endpointIds = endpointIds;
        }
    }

    public static class PolicySummary {
        private Long id;
        private String name;
        private String description;
        private String type;
        private String policyType;
        private String conditions;
        private Boolean isActive;

        public static PolicySummary from(Policy policy) {
            PolicySummary summary = new PolicySummary();
            summary.setId(policy.getId());
            summary.setName(policy.getName());
            summary.setDescription(policy.getDescription());
            summary.setType(policy.getType());
            summary.setPolicyType(policy.getPolicyType());
            summary.setConditions(policy.getConditions());
            summary.setIsActive(policy.getIsActive());
            return summary;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPolicyType() {
            return policyType;
        }

        public void setPolicyType(String policyType) {
            this.policyType = policyType;
        }

        public String getConditions() {
            return conditions;
        }

        public void setConditions(String conditions) {
            this.conditions = conditions;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }
}
