package com.example.userauth.dto;

import com.example.userauth.entity.AuthLevel;
import com.example.userauth.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    private UserRole role;
    
    @NotBlank(message = "Board ID is required")
    @Size(min = 1, max = 64, message = "Board ID must be between 1 and 64 characters")
    private String boardId;
    
    @Size(max = 64, message = "Employer ID must be at most 64 characters")
    private String employerId;

    @NotBlank(message = "Toli ID is required")
    @Size(min = 1, max = 64, message = "Toli ID must be between 1 and 64 characters")
    private String toliId;

    @NotBlank(message = "User type is required")
    @Pattern(regexp = "EMPLOYEE|EMPLOYER|BOARD|ADMIN", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Invalid user type")
    private String userType;

    private AuthLevel authLevel = AuthLevel.NONE;

    @JsonAlias({ "isActive", "enabled" })
    private Boolean enabled;

    @JsonAlias({ "canread", "canRead" })
    private Boolean canRead;

    @JsonAlias({ "canwrite", "canWrite" })
    private Boolean canWrite;

    private Boolean accountNonExpired;

    private Boolean accountNonLocked;

    private Boolean credentialsNonExpired;
    
    // Constructors
    public RegisterRequest() {}
    
    public RegisterRequest(String username, String email, String password, String fullName, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }
    
    public RegisterRequest(String username, String email, String password, String fullName, UserRole role, String boardId, String employerId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.boardId = boardId;
        this.employerId = employerId;
    }
    
    // Getters and Setters
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
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

    public AuthLevel getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(AuthLevel authLevel) {
        this.authLevel = authLevel;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAccountNonExpired() {
        return accountNonExpired;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public Boolean getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(Boolean canWrite) {
        this.canWrite = canWrite;
    }

    public void setAccountNonExpired(Boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(Boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(Boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }
}
