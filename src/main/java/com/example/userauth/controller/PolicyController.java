package com.example.userauth.controller;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.entity.Policy;
import com.example.userauth.entity.Role;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RoleRepository;
import com.example.userauth.repository.RolePolicyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.shared.common.annotation.Auditable;

/**
 * Admin controller for managing policies and their role assignments
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/policies")
@SecurityRequirement(name = "Bearer Authentication")
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final PolicyRepository policyRepository;
    private final EndpointRepository endpointRepository;
    private final RoleRepository roleRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public PolicyController(
            PolicyRepository policyRepository,
            EndpointRepository endpointRepository,
            RoleRepository roleRepository,
            RolePolicyRepository rolePolicyRepository,
            EndpointPolicyRepository endpointPolicyRepository) {
        this.policyRepository = policyRepository;
        this.endpointRepository = endpointRepository;
        this.roleRepository = roleRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

    /**
     * Get all policies with their assigned roles
     */
    @Auditable(action = "GET_ALL_POLICIES", resourceType = "POLICY")
    @GetMapping
    @Transactional(readOnly = true)
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
     * Get policy by ID with assigned roles
     */
    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPolicyById(@PathVariable Long id, HttpServletRequest request) {
        return policyRepository.findById(id)
                .map(policy -> {
                    Map<String, Object> response = convertToResponse(policy);
                    try {
                        String responseJson = objectMapper.writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag).body(response);
                    } catch (Exception e) {
                        logger.error("Error processing policy response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new policy
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody PolicyRequest request) {
        // Validate roles in expression if it's RBAC type
        if ("RBAC".equalsIgnoreCase(request.getType()) || request.getType() == null) {
            validateRolesInExpression(request.getExpression());
        }

        Policy policy = new Policy(
                request.getName(),
                request.getDescription(),
                request.getType() != null ? request.getType() : "ROLE_BASED",
                request.getExpression()
        );
        Long nextId = policyRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);
        policy.setId(nextId);
        policy.setIsActive(request.getIsActive());
        Policy saved = policyRepository.save(policy);

        return ResponseEntity.ok(convertToResponse(saved));
    }

    /**
     * Validate that all roles in the policy expression exist in the database
     */
    private void validateRolesInExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        try {
            JsonNode policyExpression = objectMapper.readTree(expression);
            if (policyExpression.has("roles") && policyExpression.get("roles").isArray()) {
                for (JsonNode roleNode : policyExpression.get("roles")) {
                    String roleName = roleNode.asText();
                    if (!roleRepository.existsByName(roleName)) {
                        throw new IllegalArgumentException("Role '" + roleName + "' does not exist");
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid policy expression or role not found: " + e.getMessage());
        }
    }

    /**
     * Update policy
     */
    @PutMapping("/{id}")
    @Transactional
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
                    // Validate roles in expression if it's RBAC type
                    if ("RBAC".equalsIgnoreCase(request.getType()) || request.getType() == null) {
                        validateRolesInExpression(request.getExpression());
                    }
                    policy.setExpression(request.getExpression());
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

                            long nextId = endpointPolicyRepository.findTopByOrderByIdDesc()
                                    .map(existing -> existing.getId() + 1)
                                    .orElse(1L);

                            for (Long endpointId : endpointIds) {
                                Endpoint endpoint = endpointById.get(endpointId);
                                EndpointPolicy endpointPolicy = new EndpointPolicy(endpoint, policy);
                                endpointPolicy.setId(nextId++);
                                endpointPolicyRepository.save(endpointPolicy);
                            }
                        }

                        return ResponseEntity.ok(convertToResponse(policy));
                    } catch (IllegalArgumentException e) {
                        logger.error("Failed to update endpoints for policy {}", id, e);
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "error", e.getMessage(),
                                        "policyId", id
                                ));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete policy
     */
    @DeleteMapping("/{id}")
    @Transactional
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
        
        // Add roles that have this policy assigned
        List<Role> roles = rolePolicyRepository.findRolesByPolicyId(policy.getId());
        List<Map<String, Object>> roleList = roles.stream()
                .map(role -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", role.getId());
                    roleMap.put("name", role.getName());
                    roleMap.put("description", role.getDescription());
                    return roleMap;
                })
                .collect(Collectors.toList());
        response.put("roles", roleList);

        List<EndpointPolicy> endpointPolicies = endpointPolicyRepository.findByPolicyId(policy.getId());
        List<Map<String, Object>> endpoints = endpointPolicies.stream()
                .map(EndpointPolicy::getEndpoint)
                .filter(Objects::nonNull)
                .map(endpoint -> {
                    Map<String, Object> endpointMap = new HashMap<>();
                    endpointMap.put("id", endpoint.getId());
                    endpointMap.put("service", endpoint.getService());
                    endpointMap.put("version", endpoint.getVersion());
                    endpointMap.put("method", endpoint.getMethod());
                    endpointMap.put("path", endpoint.getPath());
                    endpointMap.put("description", endpoint.getDescription());
                    return endpointMap;
                })
                .collect(Collectors.toList());
        response.put("endpoints", endpoints);
        
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
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
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
