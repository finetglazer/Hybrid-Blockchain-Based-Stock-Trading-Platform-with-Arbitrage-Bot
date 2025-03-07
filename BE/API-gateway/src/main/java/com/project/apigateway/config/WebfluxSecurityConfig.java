package com.project.apigateway.config;

import com.project.apigateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebfluxSecurityConfig {

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/forgot-password").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/register").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
