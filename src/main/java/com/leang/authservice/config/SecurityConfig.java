package com.leang.authservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Value("${keycloak.client-id:oauth-admin-client}")
    private String keycloakClientId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auths/**",
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/profiles/register",
                                "/api/v1/files/**",
                                "/error",
                                "/api/v1/bakong/**",
                                "/api/v1/notifications/sendMessageToAllUsers",
                                "/api/v1/notifications/sendMessageToUser/**",
                                "/api/v1/guest/**",
                                "/api/v1/order-items/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/history").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/guest-checkout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*").permitAll()
                        // Admin-only management
                        .requestMatchers("/api/v1/admin/dashboard/**").hasRole("admin")
                        .requestMatchers("/api/v1/admin/statistics/**").hasRole("admin")
                        .requestMatchers("/api/v1/admin/users/**").hasRole("admin")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/products/**").hasRole("admin")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/products/**").hasRole("admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/products/**").hasRole("admin")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/products/**").hasAnyRole("admin", "cashier")
                        // Staff order fulfillment (admin + cashier)
                        .requestMatchers("/api/v1/admin/orders/**").hasAnyRole("admin", "cashier")
                        .requestMatchers("/api/v1/cashier/**").hasAnyRole("admin", "cashier")
                        // Remaining admin routes
                        .requestMatchers("/api/v1/admin/**").hasRole("admin")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
                realmRoles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }

            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get(keycloakClientId);
                if (clientRoles != null && clientRoles.get("roles") instanceof List<?> clientRoleList) {
                    clientRoleList.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                }
            }

            return authorities;
        });

        return converter;
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().println("{ \"error\": \"Unauthorized access\" }");
        };
    }
}
