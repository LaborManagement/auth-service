package com.example.userauth.service;

import com.example.userauth.entity.Policy;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RolePolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Policy Engine Service - Evaluates RBAC policies for authorization decisions
 * 
 * Updated to use RolePolicy junction table instead of JSON expression evaluation.
 * Now policies are directly assigned to roles via the role_policies table.
 */
@Service
public class PolicyEngineService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEngineService.class);
    
    private final PolicyRepository policyRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final ObjectMapper objectMapper;

    public PolicyEngineService(PolicyRepository policyRepository, 
                               RolePolicyRepository rolePolicyRepository,
                               ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate if user with given roles can access an endpoint
     * 
     * NEW APPROACH: Check if any of the user's roles have policies that grant access to this endpoint.
     * 
     * @param endpointId The endpoint to check access for
     * @param userRoles The roles the user has
     * @return true if access is granted, false otherwise
     */
    public boolean evaluateEndpointAccess(Long endpointId, Set<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            logger.debug("No roles provided for endpoint access check");
            return false;
        }

        // Get all policies assigned to the user's roles
        List<Policy> userPolicies = rolePolicyRepository.findPoliciesByRoleNames(
                userRoles.stream().toList());
        
        if (userPolicies.isEmpty()) {
            logger.debug("No policies found for roles: {}", userRoles);
            return false;
        }

        // Get policies that grant access to this specific endpoint
        List<Policy> endpointPolicies = policyRepository.findByEndpointId(endpointId);
        
        if (endpointPolicies.isEmpty()) {
            logger.warn("No policies found for endpoint ID: {}", endpointId);
            return false;
        }

        // Check if any of the user's policies match the endpoint's policies
        for (Policy userPolicy : userPolicies) {
            for (Policy endpointPolicy : endpointPolicies) {
                if (userPolicy.getId().equals(endpointPolicy.getId()) && userPolicy.getIsActive()) {
                    logger.debug("Access granted by policy: {} for endpoint: {}", 
                            userPolicy.getName(), endpointId);
                    return true;
                }
            }
        }

        logger.debug("Access denied for endpoint: {} with roles: {}", endpointId, userRoles);
        return false;
    }

    /**
     * Evaluate if user with given roles satisfies a page access policy.
     * Pages inherit access from the actions/endpoints they expose.
     *
     * @param pageId The UI page to check access for
     * @param userRoles The roles the user has
     * @return true if access is granted, false otherwise
     */
    public boolean evaluatePageAccess(Long pageId, Set<String> userRoles) {
        // For now, allow all authenticated users to access pages
        // Real authorization happens at the action level via endpoint policies
        logger.debug("Page access evaluation for pageId: {} with roles: {}", pageId, userRoles);
        return userRoles != null && !userRoles.isEmpty();
    }

    /**
     * DEPRECATED: Old expression-based policy evaluation
     * Kept for backward compatibility during migration period.
     * @deprecated Use role-based policy assignment via RolePolicy instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    private boolean evaluatePolicy(Policy policy, Set<String> userRoles) {
        try {
            // Only use this if expression field still exists (backward compatibility)
            if (policy.getExpression() == null) {
                logger.warn("Policy {} has no expression - use RolePolicy assignment instead", 
                        policy.getName());
                return false;
            }
            
            JsonNode policyExpression = objectMapper.readTree(policy.getExpression());
            
            // Handle RBAC policy type
            if ("RBAC".equalsIgnoreCase(policy.getType())) {
                return evaluateRBACPolicy(policyExpression, userRoles);
            }
            
            // Handle ABAC policy type (future enhancement)
            if ("ABAC".equalsIgnoreCase(policy.getType())) {
                logger.warn("ABAC policies not yet implemented, defaulting to deny");
                return false;
            }
            
            logger.warn("Unknown policy type: {}", policy.getType());
            return false;
            
        } catch (Exception e) {
            logger.error("Error evaluating policy: {}", policy.getName(), e);
            return false;
        }
    }

    /**
     * DEPRECATED: Old RBAC policy expression evaluation
     * @deprecated Use role-based policy assignment via RolePolicy instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    private boolean evaluateRBACPolicy(JsonNode policyExpression, Set<String> userRoles) {
        if (!policyExpression.has("roles")) {
            logger.warn("RBAC policy missing 'roles' field");
            return false;
        }

        JsonNode rolesNode = policyExpression.get("roles");
        if (!rolesNode.isArray()) {
            logger.warn("RBAC policy 'roles' field is not an array");
            return false;
        }

        // Check if user has ANY of the required roles (OR logic)
        for (JsonNode roleNode : rolesNode) {
            String requiredRole = roleNode.asText();
            if (userRoles.contains(requiredRole)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all policies for a specific endpoint
     * 
     * @param endpointId The endpoint ID
     * @return List of policies
     */
    public List<Policy> getPoliciesForEndpoint(Long endpointId) {
        return policyRepository.findByEndpointId(endpointId);
    }

    /**
     * Get all active policies
     * 
     * @return List of all active policies
     */
    public List<Policy> getAllActivePolicies() {
        return policyRepository.findByIsActiveTrue();
    }
}
