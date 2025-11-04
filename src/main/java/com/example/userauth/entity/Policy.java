package com.example.userauth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shared.entityaudit.annotation.EntityAuditEnabled;
import com.shared.entityaudit.descriptor.AbstractAuditableEntity;
import com.shared.entityaudit.listener.SharedEntityAuditListener;

/**
 * Represents an authorization policy in the system.
 * Policies define which endpoints are accessible.
 * Assigned to roles via the role_policies junction table (RolePolicy entity).
 *
 * Migration Note: The 'expression' JSON field has been removed.
 * Role assignments are now handled through the RolePolicy relationship.
 */
@Entity
@EntityAuditEnabled
@EntityListeners(SharedEntityAuditListener.class)
@Table(name = "policies")
public class Policy extends AbstractAuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String type; // RBAC, ABAC, CUSTOM

    /**
     * DEPRECATED: JSON expression field - will be removed after migration.
     * Use RolePolicy relationship instead.
     * NOTE: Column has been renamed to 'expression_deprecated' in the database.
     * @deprecated Use {@link RolePolicy} for role-policy assignments
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expression_deprecated", columnDefinition = "jsonb")
    private String expression;

    /**
     * Policy type for fine-grained categorization
     * Examples: PERMISSION, CONDITIONAL, ROW_LEVEL, TIME_BASED
     */
    @Column(name = "policy_type", length = 50)
    private String policyType = "PERMISSION";

    /**
     * Optional ABAC conditions for advanced authorization scenarios
     * Examples:
     * - {"tenant_id": 123} - Tenant-specific policy
     * - {"time_range": "09:00-17:00"} - Time-based access
     * - {"ip_whitelist": ["192.168.1.0/24"]} - IP-based restrictions
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship with Role (Policies are assigned to Roles)
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<RolePolicy> rolePolicies = new HashSet<>();

    public Policy() {
    }

    /**
     * Constructor for creating a policy with expression (deprecated).
     * @deprecated Use constructor without expression parameter
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public Policy(String name, String description, String type, String expression) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.expression = expression;
        this.isActive = true;
    }

    /**
     * Constructor for creating a policy with policy type
     */
    public Policy(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.policyType = "PERMISSION";
        this.isActive = true;
    }

    /**
     * Full constructor with policy type and conditions
     */
    public Policy(String name, String description, String type, String policyType, String conditions) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.policyType = policyType;
        this.conditions = conditions;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    /**
     * @deprecated Use RolePolicy relationship instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public String getExpression() {
        return expression;
    }

    /**
     * @deprecated Use RolePolicy relationship instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void setExpression(String expression) {
        this.expression = expression;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<RolePolicy> getRolePolicies() {
        return rolePolicies;
    }

    public void setRolePolicies(Set<RolePolicy> rolePolicies) {
        this.rolePolicies = rolePolicies;
    }

    /**
     * Helper method to get all roles assigned to this policy
     */
    public Set<Role> getRoles() {
        return rolePolicies.stream()
                .filter(rp -> rp.getIsActive())
                .map(RolePolicy::getRole)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public String entityType() {
        return "POLICY";
    }

    @Override
    @JsonIgnore
    @Transient
    public Map<String, Object> auditState() {
        return auditStateOf(
                "id", id,
                "name", name,
                "description", description,
                "type", type,
                "policyType", policyType,
                "conditions", conditions,
                "isActive", isActive,
                "createdAt", createdAt != null ? createdAt.toString() : null,
                "updatedAt", updatedAt != null ? updatedAt.toString() : null
        );
    }

    @Override
    public String toString() {
        return "Policy{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
