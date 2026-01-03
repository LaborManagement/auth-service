package com.example.userauth.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import com.example.userauth.entity.Policy;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RolePolicyRepository;
import com.example.userauth.repository.EndpointPolicyRepository;
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
 * Admin controller for managing policies and their role assignments
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/auth-service/api/admin/policies")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Policy Management", description = "Admin APIs for managing policies and their role assignments.")
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final PolicyRepository policyRepository;
    private final EndpointRepository endpointRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public PolicyController(
            PolicyRepository policyRepository,
            EndpointRepository endpointRepository,
            RolePolicyRepository rolePolicyRepository,
            EndpointPolicyRepository endpointPolicyRepository) {
        this.policyRepository = policyRepository;
        this.endpointRepository = endpointRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

    /**
     * Get all policies without role/endpoint assignments
     */
    @Auditable(action = "GET_ALL_POLICIES", resourceType = "POLICY")
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all policies", description = "Returns all policies without assignment details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policies retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Map<String, Object>>> getAllPolicies(HttpServletRequest request) {
        List<Policy> policies = policyRepository.findAll();
        List<Map<String, Object>> response = policies.stream()
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
            logger.error("Error processing policies response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get policy by ID without assignments
     */
    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Operation(summary = "Get policy by ID", description = "Returns a policy by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy found and returned"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getPolicyById(@PathVariable Long id, HttpServletRequest request) {
        return policyRepository.findById(id)
                .map(policy -> {
                    Map<String, Object> response = convertToResponse(policy);
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
                        logger.error("Error processing policy response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity
                                .internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get endpoints assigned to a policy
     */
    @GetMapping("/{id}/endpoints")
    @Transactional(readOnly = true)
    @Operation(summary = "Get endpoints for a policy", description = "Returns endpoints linked to the specified policy.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoints retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Map<String, Object>>> getPolicyEndpoints(@PathVariable Long id, HttpServletRequest request) {
        if (!policyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<EndpointPolicy> endpointPolicies = endpointPolicyRepository.findByPolicyId(id);
        List<Map<String, Object>> endpoints = endpointPolicies.stream()
                .map(EndpointPolicy::getEndpoint)
                .filter(endpoint -> endpoint != null)
                .map(endpoint -> {
                    Map<String, Object> endpointMap = new HashMap<>();
                    endpointMap.put("id", endpoint.getId());
                    endpointMap.put("service", endpoint.getService());
                    endpointMap.put("version", endpoint.getVersion());
                    endpointMap.put("method", endpoint.getMethod());
                    endpointMap.put("path", endpoint.getPath());
                    endpointMap.put("description", endpoint.getDescription());
                    endpointMap.put("module", endpoint.getModule());
                    endpointMap.put("isActive", endpoint.getIsActive());
                    return endpointMap;
                })
                .collect(Collectors.toList());

        try {
            String responseJson = objectMapper.writeValueAsString(endpoints);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(endpoints);
        } catch (Exception e) {
            logger.error("Error processing policy endpoints response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new policy
     */
    @PostMapping
    @Transactional
    @Operation(summary = "Create new policy", description = "Creates a new policy.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody PolicyRequest request) {
        Policy policy = new Policy(
                request.getName(),
                request.getDescription(),
                request.getType() != null ? request.getType() : "ROLE_BASED");
        Long nextId = policyRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);
        policy.setId(nextId);
        policy.setIsActive(request.getIsActive());
        Policy saved = policyRepository.save(policy);
        return ResponseEntity.ok(convertToResponse(saved));
    }

    /**
     * // ...expression validation removed...
     * 
     * /**
     * Update policy
     */
    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update policy", description = "Updates an existing policy by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy updated successfully"),
            @ApiResponse(responseCode = "404", description = "Policy not found")
    })
    public ResponseEntity<Map<String, Object>> updatePolicy(
            @PathVariable Long id,
            @RequestBody PolicyRequest request) {

        return policyRepository.findById(id)
                .map(policy -> {
                    policy.setName(request.getName());
                    policy.setDescription(request.getDescription());
                    if (request.getType() != null) {
                        policy.setType(request.getType());
                    }
                    policy.setIsActive(request.getIsActive());
                    policyRepository.save(policy);
                    return ResponseEntity.ok(convertToResponse(policy));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Replace endpoints protected by a policy
     */
    @PutMapping("/{id}/endpoints")
    @Transactional
    @Auditable(action = "UPDATE_POLICY_ENDPOINTS", resourceType = "POLICY")
    @Operation(summary = "Replace endpoints protected by a policy", description = "Replaces the endpoints protected by a policy.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy endpoints updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or endpoint not found"),
            @ApiResponse(responseCode = "404", description = "Policy not found")
    })
    public ResponseEntity<? extends Map<String, ? extends Object>> updatePolicyEndpoints(
            @PathVariable Long id,
            @RequestBody PolicyEndpointAssignmentRequest request) {

        return policyRepository.findById(id)
                .map(policy -> {
                    try {
                        Set<Long> endpointIds = request.getEndpointIds();
                        if (endpointIds == null) {
                            endpointIds = Collections.emptySet();
                        }

                        List<Endpoint> endpoints = endpointIds.isEmpty()
                                ? Collections.emptyList()
                                : endpointRepository.findByIdIn(new ArrayList<>(endpointIds));

                        if (!endpointIds.isEmpty()) {
                            Set<Long> foundIds = endpoints.stream()
                                    .map(Endpoint::getId)
                                    .collect(Collectors.toSet());
                            Set<Long> missing = endpointIds.stream()
                                    .filter(endpointId -> !foundIds.contains(endpointId))
                                    .collect(Collectors.toSet());
                            if (!missing.isEmpty()) {
                                throw new IllegalArgumentException("Endpoint(s) not found: " + missing);
                            }
                        }

                        endpointPolicyRepository.deleteByPolicyId(id);

                        if (!endpoints.isEmpty()) {
                            Map<Long, Endpoint> endpointById = endpoints.stream()
                                    .collect(Collectors.toMap(Endpoint::getId, endpoint -> endpoint));

                            for (Long endpointId : endpointIds) {
                                Endpoint endpoint = endpointById.get(endpointId);
                                endpointPolicyRepository.save(new EndpointPolicy(endpoint, policy));
                            }
                        }

                        return ResponseEntity.ok(convertToResponse(policy));
                    } catch (IllegalArgumentException e) {
                        logger.error("Failed to update endpoints for policy {}", id, e);
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "error", e.getMessage(),
                                        "policyId", id));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete policy
     */
    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Delete policy", description = "Deletes a policy by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Policy deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Policy not found")
    })
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        if (policyRepository.existsById(id)) {
            // Delete policy links first
            endpointPolicyRepository.deleteByPolicyId(id);
            rolePolicyRepository.deleteByPolicyId(id);
            // Delete policy
            policyRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle policy active status
     */
    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle policy active status", description = "Toggles the active status of a policy by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy status toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Policy not found")
    })
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        return policyRepository.findById(id)
                .map(policy -> {
                    policy.setIsActive(!policy.getIsActive());
                    Policy updated = policyRepository.save(policy);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> convertToResponse(Policy policy) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", policy.getId());
        response.put("name", policy.getName());
        response.put("description", policy.getDescription());
        response.put("type", policy.getType());
        response.put("isActive", policy.getIsActive());
        response.put("createdAt", policy.getCreatedAt());
        response.put("updatedAt", policy.getUpdatedAt());

        return response;
    }

    // DTO classes

    public static class PolicyRequest {
        private String name;
        private String description;
        private String type;
        private String expression;
        private Boolean isActive = true;

        // Getters and Setters
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

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }

    public static class PolicyEndpointAssignmentRequest {
        private Set<Long> endpointIds;

        public Set<Long> getEndpointIds() {
            return endpointIds;
        }

        public void setEndpointIds(Set<Long> endpointIds) {
            this.endpointIds = endpointIds;
        }
    }
}
