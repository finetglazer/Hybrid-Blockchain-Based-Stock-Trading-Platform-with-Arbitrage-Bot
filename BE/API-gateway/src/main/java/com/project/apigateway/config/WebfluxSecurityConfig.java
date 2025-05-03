package com.project.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebFluxSecurity
public class WebfluxSecurityConfig {

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Allow OPTIONS requests for all endpoints
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/forgot-password").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/2fa/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/users/api/v1/auth/verify").permitAll()
                        .pathMatchers(HttpMethod.POST, "/users/api/v1/auth/reset-password").permitAll()
                        .pathMatchers("/ws/**").permitAll()  // Add this line for WebSockets
                        .pathMatchers("/market-data/ws/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://127.0.0.1:5173", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Sec-WebSocket-Key",
                "Sec-WebSocket-Version",
                "Sec-WebSocket-Extensions",
                "Upgrade",
                "Connection"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Sec-WebSocket-Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
