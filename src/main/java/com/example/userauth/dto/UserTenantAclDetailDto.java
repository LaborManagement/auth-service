package com.example.userauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing enriched user tenant ACL details with employer and toli
 * information.
 * Used by GET /auth-service/api/user-tenant-acl/{userId} endpoint.
 */
@Schema(description = "Enriched user tenant ACL details with employer and toli information")
public class UserTenantAclDetailDto {

    @Schema(description = "Access state: ASSIGNED (user has access) or PENDING (available but not assigned)", example = "ASSIGNED")
    private String state;

    @Schema(description = "User ID", example = "552")
    private Long userId;

    @Schema(description = "Username", example = "john.doe")
    private String username;

    @Schema(description = "Employer ID", example = "1001")
    private Long employerId;

    @Schema(description = "Employer registration number", example = "EMP-2024-001")
    private String employerRegNo;

    @Schema(description = "Establishment name", example = "ABC Manufacturing Ltd")
    private String establishmentName;

    @Schema(description = "Toli ID", example = "2001")
    private Long toliId;

    @Schema(description = "Toli registration number", example = "TOLI-2024-001")
    private String toliRegNo;

    @Schema(description = "Employer name in English (from toli master)", example = "ABC Manufacturing Ltd")
    private String employerNameEnglish;

    @Schema(description = "Can read flag from user_tenant_acl", example = "true")
    private Boolean canRead;

    @Schema(description = "Can write flag from user_tenant_acl", example = "false")
    private Boolean canWrite;

    // Constructors
    public UserTenantAclDetailDto() {
    }

    public UserTenantAclDetailDto(String state, Long userId, String username, Long employerId, String employerRegNo,
            String establishmentName, Long toliId, String toliRegNo,
            String employerNameEnglish, Boolean canRead, Boolean canWrite) {
        this.state = state;
        this.userId = userId;
        this.username = username;
        this.employerId = employerId;
        this.employerRegNo = employerRegNo;
        this.establishmentName = establishmentName;
        this.toliId = toliId;
        this.toliRegNo = toliRegNo;
        this.employerNameEnglish = employerNameEnglish;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    // Getters and Setters
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getEmployerId() {
        return employerId;
    }

    public void setEmployerId(Long employerId) {
        this.employerId = employerId;
    }

    public String getEmployerRegNo() {
        return employerRegNo;
    }

    public void setEmployerRegNo(String employerRegNo) {
        this.employerRegNo = employerRegNo;
    }

    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }

    public Long getToliId() {
        return toliId;
    }

    public void setToliId(Long toliId) {
        this.toliId = toliId;
    }

    public String getToliRegNo() {
        return toliRegNo;
    }

    public void setToliRegNo(String toliRegNo) {
        this.toliRegNo = toliRegNo;
    }

    public String getEmployerNameEnglish() {
        return employerNameEnglish;
    }

    public void setEmployerNameEnglish(String employerNameEnglish) {
        this.employerNameEnglish = employerNameEnglish;
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

    @Override
    public String toString() {
        return "UserTenantAclDetailDto{" +
                "state='" + state + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", employerId=" + employerId +
                ", employerRegNo='" + employerRegNo + '\'' +
                ", establishmentName='" + establishmentName + '\'' +
                ", toliId=" + toliId +
                ", toliRegNo='" + toliRegNo + '\'' +
                ", employerNameEnglish='" + employerNameEnglish + '\'' +
                ", canRead=" + canRead +
                ", canWrite=" + canWrite +
                '}';
    }
}
