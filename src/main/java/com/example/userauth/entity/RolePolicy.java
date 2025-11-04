package com.example.userauth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shared.entityaudit.annotation.EntityAuditEnabled;
import com.shared.entityaudit.descriptor.AbstractAuditableEntity;
import com.shared.entityaudit.listener.SharedEntityAuditListener;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Junction entity for Many-to-Many relationship between Role and Policy.
 * This replaces the old JSON expression-based policy assignment.
 * 
 * Architecture: Role → RolePolicy → Policy → EndpointPolicy → Endpoint
 * 
 * Benefits:
 * - Clean relational model (no JSON parsing)
 * - UI-friendly (easy dropdowns and multi-selects)
 * - Efficient queries (JOINs instead of LIKE on JSON)
 * - Auditable (track who assigned what and when)
 */
@Entity
@EntityAuditEnabled
@EntityListeners(SharedEntityAuditListener.class)
@Table(name = "role_policies", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "policy_id"}),
       indexes = {
           @Index(name = "idx_role_policies_role_id", columnList = "role_id"),
           @Index(name = "idx_role_policies_policy_id", columnList = "policy_id"),
           @Index(name = "idx_role_policies_active", columnList = "is_active")
       })
public class RolePolicy extends AbstractAuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnore // Prevent circular serialization
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Optional conditions for conditional policy assignment (ABAC scenarios)
     * Examples:
     * - {"tenant_id": 123} - Policy applies only for specific tenant
     * - {"valid_until": "2025-12-31"} - Time-bound policy
     * - {"department": "finance"} - Department-specific policy
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @Column(name = "priority")
    private Integer priority; // For policy precedence when conflicts arise

    // Constructors
    public RolePolicy() {
        this.isActive = true;
    }

    public RolePolicy(Role role, Policy policy) {
        this();
        this.role = role;
        this.policy = policy;
        this.assignedAt = LocalDateTime.now();
    }

    public RolePolicy(Role role, Policy policy, User assignedBy) {
        this(role, policy);
        this.assignedBy = assignedBy;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePolicy)) return false;
        RolePolicy that = (RolePolicy) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "RolePolicy{" +
                "id=" + id +
                ", roleId=" + (role != null ? role.getId() : null) +
                ", policyId=" + (policy != null ? policy.getId() : null) +
                ", isActive=" + isActive +
                ", assignedAt=" + assignedAt +
                '}';
    }
}
