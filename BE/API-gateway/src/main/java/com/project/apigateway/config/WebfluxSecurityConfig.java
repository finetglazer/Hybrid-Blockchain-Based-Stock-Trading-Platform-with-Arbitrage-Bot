package com.project.apigateway.config;

import com.project.apigateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.WebFilter;

@Configuration
@EnableWebFluxSecurity // <--- Important!
public class WebfluxSecurityConfig {

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http,
                                                       JwtAuthenticationFilter jwtAuthenticationFilter) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Permit some public endpoints
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/login", "/users/api/v1/auth/register", "users/api/v1/auth/forgot-password", "users/api/v1/auth/reset-password").permitAll() //mean: this will let url: /auth/login to be accessed without authentication
                        .pathMatchers("/users/api/v1/auth/public/**").permitAll()
                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )
                // Here we add our custom JWT filter
                .build();
    }
}
