package com.example.userauth.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userauth.dto.AuthorizationMatrix;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRoleAssignment;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.RolePolicyRepository;
import com.example.userauth.repository.UserRepository;
import com.example.userauth.repository.UserRoleAssignmentRepository;

/**
 * Handles role/policy resolution and accessible endpoint lookup.
 * Extracted from AuthorizationService to reduce size while keeping API
 * unchanged.
 */
@Service
public class RoleAuthorizationService {

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public RoleAuthorizationService(UserRepository userRepository,
            UserRoleAssignmentRepository userRoleRepository,
            RolePolicyRepository rolePolicyRepository,
            EndpointPolicyRepository endpointPolicyRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePolicyRepository = rolePolicyRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

    @Transactional(readOnly = true)
    public AuthorizationMatrix buildAuthorizationMatrix(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return buildAuthorizationMatrix(user);
    }

    AuthorizationMatrix buildAuthorizationMatrix(User user) {
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
     * Get all endpoint IDs accessible to specific roles via their policies
     */
    @Transactional(readOnly = true)
    public Set<Long> getAccessibleEndpointIds(Set<String> roleNames) {
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

        return endpointIds;
    }
}
