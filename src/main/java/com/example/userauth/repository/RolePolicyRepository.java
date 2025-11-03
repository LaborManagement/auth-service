package com.example.userauth.repository;

import com.example.userauth.entity.RolePolicy;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RolePolicy entity - manages Role-Policy relationships.
 * This replaces the old JSON expression-based policy lookups.
 */
@Repository
public interface RolePolicyRepository extends JpaRepository<RolePolicy, Long> {

    /**
     * Find all role-policy assignments for a specific role
     */
    List<RolePolicy> findByRoleId(Long roleId);

    /**
     * Find all active role-policy assignments for a specific role
     */
    List<RolePolicy> findByRoleIdAndIsActiveTrue(Long roleId);

    /**
     * Find all role-policy assignments for a specific policy
     */
    List<RolePolicy> findByPolicyId(Long policyId);

    /**
     * Find all active role-policy assignments for a specific policy
     */
    List<RolePolicy> findByPolicyIdAndIsActiveTrue(Long policyId);

    /**
     * Find a specific role-policy assignment
     */
    Optional<RolePolicy> findByRoleIdAndPolicyId(Long roleId, Long policyId);

    /**
     * Find active role-policy assignment
     */
    Optional<RolePolicy> findByRoleIdAndPolicyIdAndIsActiveTrue(Long roleId, Long policyId);

    /**
     * Find all policies assigned to a role by role name
     */
    @Query("SELECT rp.policy FROM RolePolicy rp " +
           "WHERE rp.role.name = :roleName " +
           "AND rp.isActive = true " +
           "AND rp.policy.isActive = true")
    List<Policy> findPoliciesByRoleName(@Param("roleName") String roleName);

    /**
     * Find all roles that have a specific policy assigned
     */
    @Query("SELECT rp.role FROM RolePolicy rp " +
           "WHERE rp.policy.id = :policyId " +
           "AND rp.isActive = true " +
           "AND rp.role.isActive = true")
    List<Role> findRolesByPolicyId(@Param("policyId") Long policyId);

    /**
     * Find all roles that have a specific policy assigned by policy name
     */
    @Query("SELECT rp.role FROM RolePolicy rp " +
           "WHERE rp.policy.name = :policyName " +
           "AND rp.isActive = true " +
           "AND rp.role.isActive = true")
    List<Role> findRolesByPolicyName(@Param("policyName") String policyName);

    /**
     * Check if a role has a specific policy assigned
     */
    @Query("SELECT CASE WHEN COUNT(rp) > 0 THEN true ELSE false END " +
           "FROM RolePolicy rp " +
           "WHERE rp.role.id = :roleId " +
           "AND rp.policy.id = :policyId " +
           "AND rp.isActive = true")
    boolean existsByRoleIdAndPolicyIdAndIsActiveTrue(@Param("roleId") Long roleId, 
                                                      @Param("policyId") Long policyId);

    /**
     * Count active policies for a role
     */
    @Query("SELECT COUNT(rp) FROM RolePolicy rp " +
           "WHERE rp.role.id = :roleId " +
           "AND rp.isActive = true")
    long countByRoleIdAndIsActiveTrue(@Param("roleId") Long roleId);

    /**
     * Count active roles for a policy
     */
    @Query("SELECT COUNT(rp) FROM RolePolicy rp " +
           "WHERE rp.policy.id = :policyId " +
           "AND rp.isActive = true")
    long countByPolicyIdAndIsActiveTrue(@Param("policyId") Long policyId);

    /**
     * Delete all role-policy assignments for a role
     */
    void deleteByRoleId(Long roleId);

    /**
     * Delete all role-policy assignments for a policy
     */
    void deleteByPolicyId(Long policyId);

    /**
     * Find all policies for a list of role names (used in authorization)
     */
    @Query("SELECT DISTINCT rp.policy FROM RolePolicy rp " +
           "WHERE rp.role.name IN :roleNames " +
           "AND rp.isActive = true " +
           "AND rp.policy.isActive = true")
    List<Policy> findPoliciesByRoleNames(@Param("roleNames") List<String> roleNames);

    /**
     * Get policy names for a specific role
     */
    @Query("SELECT p.name FROM RolePolicy rp " +
           "JOIN rp.policy p " +
           "WHERE rp.role.id = :roleId " +
           "AND rp.isActive = true " +
           "AND p.isActive = true")
    List<String> findPolicyNamesByRoleId(@Param("roleId") Long roleId);

    /**
     * Get policy names for a specific role by role name
     */
    @Query("SELECT p.name FROM RolePolicy rp " +
           "JOIN rp.policy p " +
           "WHERE rp.role.name = :roleName " +
           "AND rp.isActive = true " +
           "AND p.isActive = true")
    List<String> findPolicyNamesByRoleName(@Param("roleName") String roleName);
}
