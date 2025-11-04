package com.example.userauth.repository;

import com.example.userauth.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Policy entity.
 * Provides methods to query policies by various criteria.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /**
     * Find a policy by its unique name
     */
    Optional<Policy> findByName(String name);

    /**
     * Find all policies of a given type (RBAC, ABAC, CUSTOM)
     */
    List<Policy> findByType(String type);

    /**
     * Find all active policies
     */
    List<Policy> findByIsActiveTrue();

    /**
     * Find active policies by type
     */
    List<Policy> findByTypeAndIsActiveTrue(String type);

    /**
     * Check if a policy exists by name
     */
    boolean existsByName(String name);

    /**
     * Find RBAC policies that apply to a specific role
     * DEPRECATED: Use RolePolicyRepository.findPoliciesByRoleName instead
     * @deprecated Use {@link RolePolicyRepository#findPoliciesByRoleName(String)} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @Query("SELECT p FROM Policy p " +
           "WHERE p.type = 'RBAC' " +
           "AND CAST(p.expression AS string) LIKE CONCAT('%', :roleName, '%') " +
           "AND p.isActive = true")
    List<Policy> findRBACPoliciesByRole(@Param("roleName") String roleName);

    /**
     * Find policies assigned to a specific role (NEW - using RolePolicy junction table)
     * This replaces findRBACPoliciesByRole
     */
    @Query("SELECT p FROM Policy p " +
           "JOIN RolePolicy rp ON rp.policy.id = p.id " +
           "JOIN Role r ON r.id = rp.role.id " +
           "WHERE r.name = :roleName " +
           "AND rp.isActive = true " +
           "AND p.isActive = true")
    List<Policy> findPoliciesByRoleName(@Param("roleName") String roleName);

    /**
     * Find policies assigned to any of the given role names
     */
    @Query("SELECT DISTINCT p FROM Policy p " +
           "JOIN RolePolicy rp ON rp.policy.id = p.id " +
           "JOIN Role r ON r.id = rp.role.id " +
           "WHERE r.name IN :roleNames " +
           "AND rp.isActive = true " +
           "AND p.isActive = true")
    List<Policy> findPoliciesByRoleNames(@Param("roleNames") List<String> roleNames);

    /**
     * Find policies linked to a specific endpoint
     */
    @Query("SELECT p FROM Policy p " +
           "JOIN EndpointPolicy ep ON ep.policy.id = p.id " +
           "WHERE ep.endpoint.id = :endpointId " +
           "AND p.isActive = true")
    List<Policy> findByEndpointId(@Param("endpointId") Long endpointId);

    Optional<Policy> findTopByOrderByIdDesc();

}
