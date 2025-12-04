package com.example.userauth.service;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.entity.Policy;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.service.support.PolicyEndpointMapping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles policy and endpoint linkage loading to avoid duplication across services.
 */
@Service
public class PolicyAuthorizationService {

    private final PolicyRepository policyRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public PolicyAuthorizationService(PolicyRepository policyRepository,
                                      EndpointRepository endpointRepository,
                                      EndpointPolicyRepository endpointPolicyRepository) {
        this.policyRepository = policyRepository;
        this.endpointRepository = endpointRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

    @Transactional(readOnly = true)
    public PolicyEndpointMapping buildPolicyEndpointMapping() {
        List<Policy> activePolicies = policyRepository.findByIsActiveTrue();
        List<Endpoint> allEndpoints = endpointRepository.findAll();

        Map<Long, Policy> activePoliciesById = activePolicies.stream()
                .collect(Collectors.toMap(Policy::getId, policy -> policy));

        Map<Long, Endpoint> activeEndpointsById = allEndpoints.stream()
                .filter(endpoint -> Boolean.TRUE.equals(endpoint.getIsActive()))
                .collect(Collectors.toMap(Endpoint::getId, endpoint -> endpoint));

        List<Long> activePolicyIds = new ArrayList<>(activePoliciesById.keySet());
        List<EndpointPolicy> endpointPolicies = activePolicyIds.isEmpty()
                ? Collections.emptyList()
                : endpointPolicyRepository.findByPolicyIdIn(activePolicyIds);

        Map<Long, Map<Long, Endpoint>> endpointsByPolicyId = new LinkedHashMap<>();
        Set<Long> allLinkedEndpointIds = new LinkedHashSet<>();
        for (EndpointPolicy endpointPolicy : endpointPolicies) {
            Policy policy = endpointPolicy.getPolicy();
            Endpoint endpoint = endpointPolicy.getEndpoint();
            if (policy == null || endpoint == null) {
                continue;
            }
            Endpoint activeEndpoint = activeEndpointsById.get(endpoint.getId());
            if (activeEndpoint == null) {
                continue;
            }
            allLinkedEndpointIds.add(activeEndpoint.getId());
            endpointsByPolicyId
                    .computeIfAbsent(policy.getId(), id -> new LinkedHashMap<>())
                    .put(activeEndpoint.getId(), activeEndpoint);
        }

        return new PolicyEndpointMapping(activePoliciesById, activeEndpointsById, endpointsByPolicyId, allLinkedEndpointIds);
    }
}
