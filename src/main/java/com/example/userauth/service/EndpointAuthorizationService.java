package com.example.userauth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import com.example.userauth.dto.EndpointAuthorizationMetadata;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;

/**
 * Handles endpoint catalog lookup, normalization, path matching, and metadata
 * resolution.
 * Extracted from AuthorizationService to keep public API stable while reducing
 * complexity.
 */
@Service
public class EndpointAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointAuthorizationService.class);
    private static final long ENDPOINT_CACHE_TTL_MS = 30_000L;

    private final EndpointRepository endpointRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, List<EndpointDescriptor>> endpointCache = new ConcurrentHashMap<>();
    private final AtomicLong endpointCacheLoadedAt = new AtomicLong(0);

    public EndpointAuthorizationService(EndpointRepository endpointRepository,
            EndpointPolicyRepository endpointPolicyRepository) {
        this.endpointRepository = endpointRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

    /**
     * Resolve the policies that guard a specific endpoint definition.
     * Returns an empty set if the endpoint is not cataloged or has no policies.
     */
    @Transactional(readOnly = true)
    public EndpointAuthorizationMetadata getEndpointAuthorizationMetadata(String httpMethod, String requestPath) {
        String normalizedMethod = httpMethod != null ? httpMethod.toUpperCase(Locale.ROOT) : "GET";
        String normalizedPath = normalizePath(requestPath);

        Optional<EndpointDescriptor> endpointOpt = findMatchingEndpoint(normalizedMethod, normalizedPath);
        if (endpointOpt.isEmpty()) {
            logger.debug("No endpoint catalog match for method={} path={}", normalizedMethod, normalizedPath);
            return new EndpointAuthorizationMetadata(false, null, false, Set.of());
        }

        EndpointDescriptor endpoint = endpointOpt.get();
        if (!endpoint.active()) {
            logger.debug("Endpoint {} is inactive, denying by default", endpoint.id());
            return new EndpointAuthorizationMetadata(true, endpoint.id(), false, Set.of());
        }

        Set<Long> policyIds = endpointPolicyRepository.findByEndpointId(endpoint.id()).stream()
                .map(ep -> ep.getPolicy() != null ? ep.getPolicy().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (policyIds.isEmpty()) {
            logger.debug("Endpoint {} has no policies linked", endpoint.id());
            return new EndpointAuthorizationMetadata(true, endpoint.id(), false, Set.of());
        }

        return new EndpointAuthorizationMetadata(true, endpoint.id(), true, policyIds);
    }

    private Optional<EndpointDescriptor> findMatchingEndpoint(String method, String normalizedPath) {
        List<EndpointDescriptor> candidates = getEndpointsForMethod(method);
        for (EndpointDescriptor endpoint : candidates) {
            if (!endpoint.active()) {
                continue;
            }
            String endpointPath = normalizePath(endpoint.path());
            if (pathMatcher.match(endpointPath, normalizedPath)) {
                return Optional.of(endpoint);
            }
            for (String candidate : buildCompositePaths(endpoint, endpointPath)) {
                if (pathMatcher.match(candidate, normalizedPath)) {
                    return Optional.of(endpoint);
                }
            }
        }
        return Optional.empty();
    }

    private List<EndpointDescriptor> getEndpointsForMethod(String method) {
        long now = System.currentTimeMillis();
        if (now - endpointCacheLoadedAt.get() >= ENDPOINT_CACHE_TTL_MS) {
            endpointCache.clear();
            endpointCacheLoadedAt.set(now);
        }
        return endpointCache.computeIfAbsent(method, this::loadEndpointsForMethod);
    }

    private List<EndpointDescriptor> loadEndpointsForMethod(String method) {
        return endpointRepository.findByMethod(method).stream()
                .map(endpoint -> new EndpointDescriptor(
                        endpoint.getId(),
                        endpoint.getPath(),
                        endpoint.getService(),
                        endpoint.getVersion(),
                        Boolean.TRUE.equals(endpoint.getIsActive())))
                .collect(Collectors.toList());
    }

    private record EndpointDescriptor(Long id, String path, String service, String version, boolean active) {
    }

    private List<String> buildCompositePaths(EndpointDescriptor endpoint, String normalizedEndpointPath) {
        if (!StringUtils.hasText(endpoint.service())) {
            return List.of();
        }

        String serviceSegment = trimSlashes(endpoint.service());
        String versionSegment = trimSlashes(endpoint.version());

        String suffix = normalizedEndpointPath.startsWith("/")
                ? normalizedEndpointPath
                : "/" + normalizedEndpointPath;

        List<String> candidates = new ArrayList<>();

        // /api/{service}/{version}{path}
        StringBuilder builder = new StringBuilder("/api/").append(serviceSegment);
        if (StringUtils.hasText(versionSegment)) {
            builder.append("/").append(versionSegment);
        }
        candidates.add(mergePath(builder.toString(), suffix));

        // /api/{service}{path}
        candidates.add(mergePath("/api/" + serviceSegment, suffix));

        // Ensure unique values and drop any equal to the original path
        return candidates.stream()
                .filter(candidate -> !candidate.equals(normalizedEndpointPath))
                .distinct()
                .toList();
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String mergePath(String base, String suffix) {
        if (base.endsWith("/") && suffix.startsWith("/")) {
            return base.substring(0, base.length() - 1) + suffix;
        }
        if (!base.endsWith("/") && !suffix.startsWith("/")) {
            return base + "/" + suffix;
        }
        return base + suffix;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
