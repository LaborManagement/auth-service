
package com.example.userauth.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for token introspection.")
public class TokenIntrospectionRequest {

    @NotBlank
    @Schema(description = "JWT token to introspect", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
