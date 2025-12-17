package com.example.userauth.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.shared.security.rls.RLSContextFilter;
import com.shared.security.rls.RLSContextManager;

/**
 * Enhanced Security Configuration with proper error handling
 * This replaces the legacy WebSecurityConfig which is now disabled
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class EnhancedSecurityConfig {

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Autowired
    private SecurityHeadersFilter securityHeadersFilter;

    @Autowired
    private DynamicEndpointAuthorizationManager dynamicEndpointAuthorizationManager;

    @Autowired
    private InternalApiAuthenticationFilter internalApiAuthenticationFilter;

    @Bean
    public RLSContextManager rlsContextManager(JdbcTemplate jdbcTemplate) {
        RLSContextManager manager = new RLSContextManager();
        // JdbcTemplate will be autowired into RLSContextManager by reflection
        return manager;
    }

    @Bean
    public RLSContextFilter rlsContextFilter(RLSContextManager rlsContextManager) {
        RLSContextFilter filter = new RLSContextFilter();
        // RLSContextManager will be autowired into the filter by reflection
        return filter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RLSContextFilter rlsContextFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow all OPTIONS requests for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout",
                                "/api/me/authorizations")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/me/authorizations")
                        .permitAll()
                        .requestMatchers("/api/auth/**").access(dynamicEndpointAuthorizationManager)
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/internal/auth/**", "/internal/authz/**").authenticated()

                        // Swagger/OpenAPI endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Actuator endpoints (if using Spring Boot Actuator)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Error endpoint should be accessible
                        .requestMatchers("/error").permitAll()

                        // System endpoints - require authentication
                        .requestMatchers("/api/system/**").authenticated()

                        // All other endpoints require authentication + dynamic RBAC enforcement
                        .anyRequest().access(dynamicEndpointAuthorizationManager));

        // Add JWT token filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // Add RLS context filter after authentication to set database context
        http.addFilterAfter(rlsContextFilter, AuthTokenFilter.class);

        // Add security headers filter
        http.addFilterBefore(securityHeadersFilter, AuthTokenFilter.class);
        http.addFilterBefore(internalApiAuthenticationFilter, AuthTokenFilter.class);

        return http.build();
    }

    @Bean(name = "authenticationManager")
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Note: PasswordEncoder bean is defined in PasswordConfig.java to avoid
    // conflicts
    // If not found, uncomment below:
    // @Bean
    // public PasswordEncoder passwordEncoder() {
    // return new BCryptPasswordEncoder();
    // }
}
