
package com.example.userauth.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for token introspection.")
public class TokenIntrospectionResponse {

    @Schema(description = "Whether the token is active/valid", example = "true")
    private boolean active;

    @Schema(description = "Subject (typically user or service identifier)", example = "user@example.com")
    private String subject;

    @Schema(description = "User ID associated with the token", example = "12345")
    private Long userId;

    @Schema(description = "Permission version for the user", example = "2")
    private Integer permissionVersion;

    @Schema(description = "Token ID (jti claim)", example = "abc123def456")
    private String tokenId;

    @Schema(description = "Token expiration timestamp (epoch seconds)", example = "1700000000")
    private Instant expiresAt;

    public static TokenIntrospectionResponse inactive() {
        TokenIntrospectionResponse response = new TokenIntrospectionResponse();
        response.setActive(false);
        return response;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPermissionVersion() {
        return permissionVersion;
    }

    public void setPermissionVersion(Integer permissionVersion) {
        this.permissionVersion = permissionVersion;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
