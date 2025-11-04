package com.example.userauth.service.dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Metadata describing the authorization requirements for a cataloged endpoint.
 * Authorization is determined by checking if the user's policies match the endpoint's required policies.
 */
public class EndpointAuthorizationMetadata {

    private final boolean endpointFound;
    private final Long endpointId;
    private final boolean hasPolicies;
    private final Set<Long> policyIds;

    public EndpointAuthorizationMetadata(boolean endpointFound,
                                         Long endpointId,
                                         boolean hasPolicies,
                                         Set<Long> policyIds) {
        this.endpointFound = endpointFound;
        this.endpointId = endpointId;
        this.hasPolicies = hasPolicies;
        this.policyIds = policyIds != null ? Collections.unmodifiableSet(new HashSet<>(policyIds)) : Set.of();
    }

    public boolean isEndpointFound() {
        return endpointFound;
    }

    public Long getEndpointId() {
        return endpointId;
    }

    public boolean hasPolicies() {
        return hasPolicies;
    }

    public boolean isHasPolicies() {
        return hasPolicies;
    }

    public Set<Long> getPolicyIds() {
        return policyIds;
    }
}
