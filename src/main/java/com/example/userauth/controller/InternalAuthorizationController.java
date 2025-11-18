package com.example.userauth.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.service.AuthorizationService;
import com.example.userauth.service.PolicyEngineService;
import com.example.userauth.service.dto.AuthorizationMatrix;
import com.example.userauth.service.dto.EndpointAuthorizationMetadata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Internal-only endpoints that expose authorization data for downstream
 * services.
 * These routes are guarded via internal network/API key strategies, so they
 * skip JWT.
 */
@RestController
@RequestMapping("/internal/authz")
@Tag(name = "Internal Authorization", description = "Internal-only endpoints for downstream service authorization and policy evaluation.")
public class InternalAuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(InternalAuthorizationController.class);

    private final AuthorizationService authorizationService;
    private final PolicyEngineService policyEngineService;
    private final EndpointRepository endpointRepository;

    public InternalAuthorizationController(AuthorizationService authorizationService,
            PolicyEngineService policyEngineService,
            EndpointRepository endpointRepository) {
        this.authorizationService = authorizationService;
        this.policyEngineService = policyEngineService;
        this.endpointRepository = endpointRepository;
    }

    /**
     * Convenience endpoint to inspect a cataloged endpoint by id.
     */
    @GetMapping("/endpoints/{endpointId}")
    @Operation(summary = "Get endpoint by ID", description = "Inspect a cataloged endpoint by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endpoint found and returned"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    public ResponseEntity<Map<String, Object>> getEndpointById(@PathVariable Long endpointId) {
        return endpointRepository.findById(endpointId)
                .map(endpoint -> ResponseEntity.ok(toEndpointResponse(endpoint)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Resolve the authorization matrix for a user (roles and policies).
     */
    @GetMapping("/users/{userId}/matrix")
    @Operation(summary = "Get authorization matrix for user", description = "Resolve the authorization matrix (roles and policies) for a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization matrix returned successfully")
    })
    public ResponseEntity<AuthorizationMatrix> getAuthorizationMatrix(@PathVariable Long userId) {
        logger.debug("Internal request for authorization matrix of user {}", userId);
        AuthorizationMatrix matrix = authorizationService.buildAuthorizationMatrix(userId);
        return ResponseEntity.ok(matrix);
    }

    private Map<String, Object> toEndpointResponse(Endpoint endpoint) {
        Map<String, Object> response = Map.of(
                "id", endpoint.getId(),
                "service", endpoint.getService(),
                "version", endpoint.getVersion(),
                "method", endpoint.getMethod(),
                "path", endpoint.getPath(),
                "description", endpoint.getDescription(),
                "isActive", endpoint.getIsActive(),
                "policyIds", endpoint.getEndpointPolicies().stream()
                        .map(EndpointPolicy::getPolicy)
                        .filter(policy -> policy != null)
                        .map(policy -> Map.of(
                                "id", policy.getId(),
                                "name", policy.getName(),
                                "type", policy.getType()))
                        .collect(Collectors.toList()));
        return response;
    }

    /**
     * Resolve endpoint authorization metadata for a given HTTP method + path
     * combination.
     * Method should be upper/lower case agnostic and path should be raw request
     * URI.
     */
    @GetMapping("/endpoints/metadata")
    @Operation(summary = "Get endpoint authorization metadata", description = "Resolve endpoint authorization metadata for a given HTTP method and path.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadata returned successfully")
    })
    public ResponseEntity<EndpointAuthorizationMetadata> getEndpointMetadata(
            @Parameter(description = "HTTP method (GET, POST, etc.)") @RequestParam("method") @NotBlank String method,
            @Parameter(description = "Request path (raw URI)") @RequestParam("path") @NotBlank String path) {
        EndpointAuthorizationMetadata metadata = authorizationService.getEndpointAuthorizationMetadata(method, path);
        return ResponseEntity.ok(metadata);
    }

    /**
     * Optional policy evaluation endpoint. Downstream callers can supply endpoint
     * id
     * plus the caller's roles to determine whether any linked policy grants access.
     */
    @PostMapping("/policies/evaluate")
    @Operation(summary = "Evaluate endpoint policy", description = "Evaluate if a set of roles grants access to an endpoint via policy.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy evaluation result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request (missing endpointId)")
    })
    public ResponseEntity<Map<String, Object>> evaluateEndpointPolicy(@RequestBody PolicyEvaluationRequest request) {
        if (request.getEndpointId() == null) {
            throw new IllegalArgumentException("endpointId is required");
        }
        Set<String> roles = request.getRoles() != null ? request.getRoles() : Set.of();
        boolean allowed = policyEngineService.evaluateEndpointAccess(request.getEndpointId(), roles);
        Map<String, Object> response = Map.of(
                "endpointId", request.getEndpointId(),
                "allowed", allowed);
        return ResponseEntity.ok(response);
    }

    /**
     * Request body for policy evaluation endpoint.
     */
    public static class PolicyEvaluationRequest {
        private Long endpointId;
        private Set<String> roles;

        public Long getEndpointId() {
            return endpointId;
        }

        public void setEndpointId(Long endpointId) {
            this.endpointId = endpointId;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
    }
}
