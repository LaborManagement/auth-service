package com.example.userauth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shared.entityaudit.annotation.EntityAuditEnabled;
import com.shared.entityaudit.descriptor.AbstractAuditableEntity;
import com.shared.entityaudit.listener.SharedEntityAuditListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Role entity for the policy-driven authorization system.
 * Roles are assigned to users and linked to policies that grant endpoint access.
 * This replaces the old role_permissions system.
 */
@Entity
@EntityAuditEnabled
@EntityListeners(SharedEntityAuditListener.class)
@Table(name = "roles")
public class Role extends AbstractAuditableEntity<Long> {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name; // e.g., "ADMIN", "RECONCILIATION_OFFICER", "WORKER", "EMPLOYER", "BOARD"
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description", length = 255)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Many-to-Many relationship with User through user_roles table
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @JsonIgnore // prevent infinite recursion during serialization
    private Set<User> users = new HashSet<>();
    
    // One-to-Many relationship with Policy through role_policies junction table
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore // prevent circular serialization
    private Set<RolePolicy> rolePolicies = new HashSet<>();
    
    @Transient
    private Set<String> policyNames = new HashSet<>();

    // Constructors
    public Role() {
        this.isActive = true;
    }
    
    public Role(String name, String description) {
        this();
        this.name = name;
        this.description = description;
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Set<User> getUsers() {
        return users;
    }
    
    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<RolePolicy> getRolePolicies() {
        return rolePolicies;
    }

    public void setRolePolicies(Set<RolePolicy> rolePolicies) {
        this.rolePolicies = rolePolicies;
    }

    /**
     * Helper method to get all active policies assigned to this role
     * NOT serialized to JSON - use policyNames instead
     */
    @JsonIgnore
    public Set<Policy> getPolicies() {
        return rolePolicies.stream()
                .filter(rp -> rp.getIsActive())
                .map(RolePolicy::getPolicy)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Set<String> getPolicyNames() {
        return policyNames;
    }

    public void setPolicyNames(Set<String> policyNames) {
        this.policyNames = policyNames != null ? policyNames : new HashSet<>();
    }
    
    // Helper methods
    public void addUser(User user) {
        this.users.add(user);
        user.getRoles().add(this);
    }
    
    public void removeUser(User user) {
        this.users.remove(user);
        user.getRoles().remove(this);
    }

    /**
     * Helper method to assign a policy to this role
     */
    public void addPolicy(Policy policy) {
        RolePolicy rolePolicy = new RolePolicy(this, policy);
        this.rolePolicies.add(rolePolicy);
        policy.getRolePolicies().add(rolePolicy);
    }

    /**
     * Helper method to assign a policy to this role with audit info
     */
    public void addPolicy(Policy policy, User assignedBy) {
        RolePolicy rolePolicy = new RolePolicy(this, policy, assignedBy);
        this.rolePolicies.add(rolePolicy);
        policy.getRolePolicies().add(rolePolicy);
    }

    /**
     * Helper method to remove a policy from this role
     */
    public void removePolicy(Policy policy) {
        rolePolicies.removeIf(rp -> rp.getPolicy().equals(policy));
        policy.getRolePolicies().removeIf(rp -> rp.getRole().equals(this));
    }
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return name != null ? name.equals(role.name) : role.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String entityType() {
        return "ROLE";
    }

    @Override
    @JsonIgnore
    @Transient
    public Map<String, Object> auditState() {
        return auditStateOf(
                "id", id,
                "name", name,
                "description", description,
                "isActive", isActive,
                "createdAt", createdAt != null ? createdAt.toString() : null,
                "updatedAt", updatedAt != null ? updatedAt.toString() : null
        );
    }
}
