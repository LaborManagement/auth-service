package com.example.userauth.service;

import com.example.userauth.entity.Policy;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.RolePolicy;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RoleRepository;
import com.example.userauth.repository.RolePolicyRepository;
import com.example.userauth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Role-Policy relationships.
 * Provides UI-friendly methods for assigning and removing policies from roles.
 */
@Service
@Transactional
public class RolePolicyService {

    private static final Logger logger = LoggerFactory.getLogger(RolePolicyService.class);

    @Autowired
    private RolePolicyRepository rolePolicyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Assign a policy to a role
     * 
     * @param roleId ID of the role
     * @param policyId ID of the policy to assign
     * @param assignedByUserId ID of the user performing the assignment (for audit)
     * @return The created RolePolicy assignment
     */
    public RolePolicy assignPolicyToRole(Long roleId, Long policyId, Long assignedByUserId) {
        logger.info("Assigning policy {} to role {}", policyId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        // Check if assignment already exists
        Optional<RolePolicy> existing = rolePolicyRepository.findByRoleIdAndPolicyId(roleId, policyId);
        if (existing.isPresent()) {
            RolePolicy rolePolicy = existing.get();
            if (rolePolicy.getIsActive()) {
                logger.warn("Policy {} already assigned to role {}", policyId, roleId);
                return rolePolicy;
            } else {
                // Reactivate if it was previously deactivated
                rolePolicy.setIsActive(true);
                rolePolicy.setAssignedAt(LocalDateTime.now());
                return rolePolicyRepository.save(rolePolicy);
            }
        }

        // Create new assignment
        RolePolicy rolePolicy = new RolePolicy();
        rolePolicy.setRole(role);
        rolePolicy.setPolicy(policy);
        rolePolicy.setAssignedAt(LocalDateTime.now());
        rolePolicy.setIsActive(true);

        if (assignedByUserId != null) {
            userRepository.findById(assignedByUserId)
                    .ifPresent(rolePolicy::setAssignedBy);
        }

        RolePolicy saved = rolePolicyRepository.save(rolePolicy);
        logger.info("Successfully assigned policy {} to role {}", policyId, roleId);
        return saved;
    }

    /**
     * Assign multiple policies to a role
     * 
     * @param roleId ID of the role
     * @param policyIds List of policy IDs to assign
     * @param assignedByUserId ID of the user performing the assignment
     * @return List of created RolePolicy assignments
     */
    public List<RolePolicy> assignPoliciesToRole(Long roleId, List<Long> policyIds, Long assignedByUserId) {
        logger.info("Assigning {} policies to role {}", policyIds.size(), roleId);
        
        return policyIds.stream()
                .map(policyId -> assignPolicyToRole(roleId, policyId, assignedByUserId))
                .collect(Collectors.toList());
    }

    /**
     * Remove a policy from a role (soft delete by setting isActive = false)
     * 
     * @param roleId ID of the role
     * @param policyId ID of the policy to remove
     */
    public void removePolicyFromRole(Long roleId, Long policyId) {
        logger.info("Removing policy {} from role {}", policyId, roleId);

        RolePolicy rolePolicy = rolePolicyRepository.findByRoleIdAndPolicyIdAndIsActiveTrue(roleId, policyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active policy assignment found for role " + roleId + " and policy " + policyId));

        rolePolicy.setIsActive(false);
        rolePolicyRepository.save(rolePolicy);
        logger.info("Successfully removed policy {} from role {}", policyId, roleId);
    }

    /**
     * Remove a policy from a role permanently (hard delete)
     * 
     * @param roleId ID of the role
     * @param policyId ID of the policy to remove
     */
    public void deletePolicyFromRole(Long roleId, Long policyId) {
        logger.info("Deleting policy {} from role {}", policyId, roleId);

        RolePolicy rolePolicy = rolePolicyRepository.findByRoleIdAndPolicyId(roleId, policyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No policy assignment found for role " + roleId + " and policy " + policyId));

        rolePolicyRepository.delete(rolePolicy);
        logger.info("Successfully deleted policy {} from role {}", policyId, roleId);
    }

    /**
     * Get all policies assigned to a role
     * 
     * @param roleId ID of the role
     * @return List of policies
     */
    @Transactional(readOnly = true)
    public List<Policy> getPoliciesForRole(Long roleId) {
        return rolePolicyRepository.findActivePoliciesByRoleId(roleId);
    }

    /**
     * Get all roles that have a specific policy assigned
     * 
     * @param policyId ID of the policy
     * @return List of roles
     */
    @Transactional(readOnly = true)
    public List<Role> getRolesForPolicy(Long policyId) {
        return rolePolicyRepository.findRolesByPolicyId(policyId);
    }

    /**
     * Get all role-policy assignments for a role
     * 
     * @param roleId ID of the role
     * @return List of RolePolicy assignments
     */
    @Transactional(readOnly = true)
    public List<RolePolicy> getRolePolicyAssignments(Long roleId) {
        return rolePolicyRepository.findByRoleIdAndIsActiveTrue(roleId);
    }

    /**
     * Replace all policies for a role with a new set
     * 
     * @param roleId ID of the role
     * @param policyIds New set of policy IDs
     * @param assignedByUserId ID of the user performing the operation
     */
    public void replacePoliciesForRole(Long roleId, Set<Long> policyIds, Long assignedByUserId) {
        logger.info("Replacing policies for role {} with {} new policies", roleId, policyIds.size());

        // Get current assignments
        List<RolePolicy> currentAssignments = rolePolicyRepository.findByRoleIdAndIsActiveTrue(roleId);
        Set<Long> currentPolicyIds = currentAssignments.stream()
                .map(rp -> rp.getPolicy().getId())
                .collect(Collectors.toSet());

        // Remove policies that are not in the new set
        for (RolePolicy assignment : currentAssignments) {
            if (!policyIds.contains(assignment.getPolicy().getId())) {
                assignment.setIsActive(false);
                rolePolicyRepository.save(assignment);
            }
        }

        // Add new policies
        for (Long policyId : policyIds) {
            if (!currentPolicyIds.contains(policyId)) {
                assignPolicyToRole(roleId, policyId, assignedByUserId);
            }
        }

        logger.info("Successfully replaced policies for role {}", roleId);
    }

    /**
     * Check if a role has a specific policy assigned
     * 
     * @param roleId ID of the role
     * @param policyId ID of the policy
     * @return true if the policy is assigned and active
     */
    @Transactional(readOnly = true)
    public boolean hasPolicy(Long roleId, Long policyId) {
        return rolePolicyRepository.existsByRoleIdAndPolicyIdAndIsActiveTrue(roleId, policyId);
    }

    /**
     * Get count of active policies for a role
     * 
     * @param roleId ID of the role
     * @return Count of active policies
     */
    @Transactional(readOnly = true)
    public long countPoliciesForRole(Long roleId) {
        return rolePolicyRepository.countByRoleIdAndIsActiveTrue(roleId);
    }

    /**
     * Get count of active roles for a policy
     * 
     * @param policyId ID of the policy
     * @return Count of active roles
     */
    @Transactional(readOnly = true)
    public long countRolesForPolicy(Long policyId) {
        return rolePolicyRepository.countByPolicyIdAndIsActiveTrue(policyId);
    }
}
