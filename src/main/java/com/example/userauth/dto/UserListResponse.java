package com.example.userauth.dto;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user list response.
 * Avoids Hibernate proxy serialization issues and provides clean API response.
 */
public class UserListResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Boolean isActive;
    private Set<RoleInfo> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String boardId;
    private String employerId;
    private String userType;
    private String toliId;

    public UserListResponse() {
    }

    public UserListResponse(Long id, String username, String email, String fullName,
            Boolean isActive, Set<RoleInfo> roles,
            LocalDateTime createdAt, LocalDateTime lastLogin,
            String boardId, String employerId, String userType, String toliId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.isActive = isActive;
        this.roles = roles;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.boardId = boardId;
        this.employerId = employerId;
        this.userType = userType;
        this.toliId = toliId;
    }

    public String getToliId() {
        return toliId;
    }

    public void setToliId(String toliId) {
        this.toliId = toliId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getEmployerId() {
        return employerId;
    }

    public void setEmployerId(String employerId) {
        this.employerId = employerId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<RoleInfo> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleInfo> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Nested DTO for role information
     */
    public static class RoleInfo {
        private Long id;
        private String name;
        private String description;
        private Set<PolicyInfo> policies;

        public RoleInfo() {
        }

        public RoleInfo(Long id, String name, String description, Set<PolicyInfo> policies) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.policies = policies;
        }

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

        public Set<PolicyInfo> getPolicies() {
            return policies;
        }

        public void setPolicies(Set<PolicyInfo> policies) {
            this.policies = policies;
        }
    }

    /**
     * Nested DTO for policy information
     */
    public static class PolicyInfo {
        private Long id;
        private String name;
        private String description;
        private String type;

        public PolicyInfo() {
        }

        public PolicyInfo(Long id, String name, String description, String type) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
        }

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
    }
}
