package com.project.apigateway.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
public class WebSocketAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        logger.debug(path);

        // Check if the path matches the WebSocket endpoint requiring this logic
        // Define the path prefix for your WebSocket endpoint(s) at the Gateway
        String webSocketAuthPath = "/market-data/ws/";

        if (path != null && path.startsWith(webSocketAuthPath)) {
            logger.debug("WebSocket path detected: {}", path);

            // Check if Authorization header already exists (maybe non-browser client)
            String AUTH_HEADER = HttpHeaders.AUTHORIZATION;
            if (request.getHeaders().containsKey(AUTH_HEADER)) {
                logger.debug("Authorization header already present, skipping query param check for path: {}", path);
                return chain.filter(exchange); // Let normal security handle it
            }

            // Extract token from query parameter
            String WEBSOCKET_TOKEN_PARAM = "token";
            List<String> tokenParams = request.getQueryParams().get(WEBSOCKET_TOKEN_PARAM);

            if (!CollectionUtils.isEmpty(tokenParams) && StringUtils.hasText(tokenParams.get(0))) {
                String token = tokenParams.get(0);
                logger.debug("Found token in query parameter for WebSocket request.");

                // --- === Replace with your actual JWT validation === ---
                boolean isValid = validateToken(token); // Your validation logic
                // --- ============================================= ---

                if (isValid) {
                    logger.debug("Token from query parameter is valid. Adding Authorization header.");
                    // Add Authorization header
                    String BEARER_PREFIX = "Bearer ";
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(AUTH_HEADER, BEARER_PREFIX + token)
                            // Optional: Remove the original query param for cleanliness downstream?
                            // .uri(UriComponentsBuilder.fromUri(request.getURI()).replaceQueryParam(WEBSOCKET_TOKEN_PARAM).build().toUri())
                            .build();

                    // Proceed with the modified request containing the Auth header
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } else {
                    logger.warn("Invalid token found in query parameter for WebSocket request. Rejecting.");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete(); // Reject the request
                }
            } else {
                // No token in query param, and no Auth header - reject WebSocket connection
                logger.warn("Missing token in query parameter and no Authorization header for WebSocket request. Rejecting.");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete(); // Reject the request
            }
        }

        // If not the specific WebSocket path, just continue the chain
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run this filter *before* Spring Security's main authentication filters
        // Check the order values of your security filters (e.g., AuthenticationWebFilter is often around -100)
        // You might need to adjust this value based on your specific filter chain.
        return Ordered.HIGHEST_PRECEDENCE; // Example: Ensure it runs just before standard auth
    }

    private boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }
    private void parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    // --- ================================== ---
}
