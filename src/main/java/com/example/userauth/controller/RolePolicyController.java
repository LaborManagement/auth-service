package com.example.userauth.controller;

import com.example.userauth.entity.Policy;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.RolePolicy;
import com.example.userauth.security.JwtUtils;
import com.example.userauth.service.RolePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for managing Role-Policy assignments.
 * Provides UI-friendly endpoints for assigning and managing policies for roles.
 */
@RestController
@RequestMapping("/auth-service/api/role-policies")
@Tag(name = "Role-Policy Management", description = "Endpoints for managing role-policy assignments")
@SecurityRequirement(name = "Bearer Authentication")
public class RolePolicyController {

    private static final Logger logger = LoggerFactory.getLogger(RolePolicyController.class);

    @Autowired
    private RolePolicyService rolePolicyService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Assign a policy to a role
     */
    @PostMapping("/assign")
    @Operation(summary = "Assign a policy to a role", 
               description = "Creates a new role-policy assignment. If the assignment already exists and is inactive, it will be reactivated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Policy successfully assigned to role"),
        @ApiResponse(responseCode = "400", description = "Invalid role or policy ID"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> assignPolicyToRole(
            @Valid @RequestBody AssignPolicyRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        logger.info("User {} assigning policy {} to role {}", userId, request.getPolicyId(), request.getRoleId());

        try {
            RolePolicy rolePolicy = rolePolicyService.assignPolicyToRole(
                    request.getRoleId(), 
                    request.getPolicyId(), 
                    userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy successfully assigned to role");
            response.put("rolePolicyId", rolePolicy.getId());
            response.put("roleId", rolePolicy.getRole().getId());
            response.put("roleName", rolePolicy.getRole().getName());
            response.put("policyId", rolePolicy.getPolicy().getId());
            response.put("policyName", rolePolicy.getPolicy().getName());
            response.put("assignedAt", rolePolicy.getAssignedAt());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error assigning policy to role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Assign multiple policies to a role
     */
    @PostMapping("/assign-multiple")
    @Operation(summary = "Assign multiple policies to a role",
               description = "Assigns multiple policies to a role in a single operation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Policies successfully assigned"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> assignMultiplePolicies(
            @Valid @RequestBody AssignMultiplePoliciesRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        logger.info("User {} assigning {} policies to role {}", 
                userId, request.getPolicyIds().size(), request.getRoleId());

        try {
            List<RolePolicy> assignments = rolePolicyService.assignPoliciesToRole(
                    request.getRoleId(), 
                    request.getPolicyIds(), 
                    userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policies successfully assigned to role");
            response.put("assignedCount", assignments.size());
            response.put("assignments", assignments.stream().map(rp -> Map.of(
                    "id", rp.getId(),
                    "policyId", rp.getPolicy().getId(),
                    "policyName", rp.getPolicy().getName()
            )).toList());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error assigning policies to role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Remove a policy from a role (soft delete)
     */
    @DeleteMapping("/remove")
    @Operation(summary = "Remove a policy from a role",
               description = "Soft deletes the role-policy assignment by marking it as inactive")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Policy successfully removed from role"),
        @ApiResponse(responseCode = "400", description = "Invalid role or policy ID"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> removePolicyFromRole(
            @RequestParam @NotNull Long roleId,
            @RequestParam @NotNull Long policyId,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        logger.info("User {} removing policy {} from role {}", userId, policyId, roleId);

        try {
            rolePolicyService.removePolicyFromRole(roleId, policyId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy successfully removed from role");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error removing policy from role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Replace all policies for a role
     */
    @PutMapping("/replace")
    @Operation(summary = "Replace all policies for a role",
               description = "Replaces all current policy assignments with a new set of policies")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Policies successfully replaced"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> replacePoliciesForRole(
            @Valid @RequestBody ReplacePoliciesRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        logger.info("User {} replacing policies for role {} with {} new policies", 
                userId, request.getRoleId(), request.getPolicyIds().size());

        try {
            rolePolicyService.replacePoliciesForRole(request.getRoleId(), request.getPolicyIds(), userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policies successfully replaced for role");
            response.put("newPolicyCount", request.getPolicyIds().size());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error replacing policies for role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all policies assigned to a role
     */
    @GetMapping("/role/{roleId}/policies")
    @Operation(summary = "Get all policies for a role",
               description = "Returns all active policies assigned to a specific role")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved policies"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getPoliciesForRole(
            @Parameter(description = "Role ID") @PathVariable Long roleId) {
        
        logger.debug("Fetching policies for role {}", roleId);

        List<Policy> policies = rolePolicyService.getPoliciesForRole(roleId);
        long count = rolePolicyService.countPoliciesForRole(roleId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roleId", roleId);
        response.put("policyCount", count);
        response.put("policies", policies.stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription() != null ? p.getDescription() : "",
                "type", p.getType(),
                "isActive", p.getIsActive()
        )).toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all roles that have a specific policy assigned
     */
    @GetMapping("/policy/{policyId}/roles")
    @Operation(summary = "Get all roles for a policy",
               description = "Returns all roles that have a specific policy assigned")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved roles"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getRolesForPolicy(
            @Parameter(description = "Policy ID") @PathVariable Long policyId) {
        
        logger.debug("Fetching roles for policy {}", policyId);

        List<Role> roles = rolePolicyService.getRolesForPolicy(policyId);
        long count = rolePolicyService.countRolesForPolicy(policyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("policyId", policyId);
        response.put("roleCount", count);
        response.put("roles", roles.stream().map(r -> Map.of(
                "id", r.getId(),
                "name", r.getName(),
                "description", r.getDescription() != null ? r.getDescription() : "",
                "isActive", r.getIsActive()
        )).toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all role-policy assignments for a role
     */
    @GetMapping("/role/{roleId}/assignments")
    @Operation(summary = "Get role-policy assignments",
               description = "Returns detailed information about all policy assignments for a role")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved assignments"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getRolePolicyAssignments(
            @Parameter(description = "Role ID") @PathVariable Long roleId) {
        
        logger.debug("Fetching role-policy assignments for role {}", roleId);

        List<RolePolicy> assignments = rolePolicyService.getRolePolicyAssignments(roleId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roleId", roleId);
        response.put("assignmentCount", assignments.size());
        response.put("assignments", assignments.stream().map(rp -> {
            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("id", rp.getId());
            assignmentData.put("policyId", rp.getPolicy().getId());
            assignmentData.put("policyName", rp.getPolicy().getName());
            assignmentData.put("policyType", rp.getPolicy().getType());
            assignmentData.put("assignedAt", rp.getAssignedAt());
            assignmentData.put("isActive", rp.getIsActive());
            if (rp.getAssignedBy() != null) {
                assignmentData.put("assignedBy", Map.of(
                        "id", rp.getAssignedBy().getId(),
                        "username", rp.getAssignedBy().getUsername()
                ));
            }
            return assignmentData;
        }).toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Check if a role has a specific policy
     */
    @GetMapping("/check")
    @Operation(summary = "Check if role has policy",
               description = "Checks if a role has a specific policy assigned and active")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully checked assignment")
    })
    public ResponseEntity<Map<String, Object>> checkRoleHasPolicy(
            @RequestParam @NotNull Long roleId,
            @RequestParam @NotNull Long policyId) {
        
        boolean hasPolicy = rolePolicyService.hasPolicy(roleId, policyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("roleId", roleId);
        response.put("policyId", policyId);
        response.put("hasPolicy", hasPolicy);

        return ResponseEntity.ok(response);
    }

    // DTO Classes

    public static class AssignPolicyRequest {
        @NotNull(message = "Role ID is required")
        private Long roleId;

        @NotNull(message = "Policy ID is required")
        private Long policyId;

        // Getters and Setters
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public Long getPolicyId() { return policyId; }
        public void setPolicyId(Long policyId) { this.policyId = policyId; }
    }

    public static class AssignMultiplePoliciesRequest {
        @NotNull(message = "Role ID is required")
        private Long roleId;

        @NotEmpty(message = "At least one policy ID is required")
        private List<Long> policyIds;

        // Getters and Setters
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public List<Long> getPolicyIds() { return policyIds; }
        public void setPolicyIds(List<Long> policyIds) { this.policyIds = policyIds; }
    }

    public static class ReplacePoliciesRequest {
        @NotNull(message = "Role ID is required")
        private Long roleId;

        @NotNull(message = "Policy IDs are required")
        private Set<Long> policyIds;

        // Getters and Setters
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public Set<Long> getPolicyIds() { return policyIds; }
        public void setPolicyIds(Set<Long> policyIds) { this.policyIds = policyIds; }
    }

    // Helper method to extract user ID from JWT token
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtils.getUserIdFromToken(token);
        }
        return null;
    }
}
