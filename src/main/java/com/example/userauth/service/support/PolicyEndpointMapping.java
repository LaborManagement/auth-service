package com.example.userauth.service.support;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.Policy;

import java.util.Map;
import java.util.Set;

/**
 * Immutable container for policy and endpoint linkage data.
 */
public record PolicyEndpointMapping(
        Map<Long, Policy> activePoliciesById,
        Map<Long, Endpoint> activeEndpointsById,
        Map<Long, Map<Long, Endpoint>> endpointsByPolicyId,
        Set<Long> allLinkedEndpointIds) {
}
