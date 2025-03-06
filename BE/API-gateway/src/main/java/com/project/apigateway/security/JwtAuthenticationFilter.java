package com.project.apigateway.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (
                path.equals("/users/api/v1/auth/login")
                        || path.equals("/users/api/v1/auth/logout")
                        || path.equals("/users/api/v1/auth/register")
                        || path.equals("/users/api/v1/auth/forgot-password")
                        || path.equals("/wallets/api/v1/coinbase-wallet/test")
                        || path.startsWith("/users/api/v1/public")
        ) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            return unauthorizedResponse(exchange, "Token has expired");
        } catch (UnsupportedJwtException e) {
            return unauthorizedResponse(exchange, "Unsupported JWT token");
        } catch (MalformedJwtException e) {
            return unauthorizedResponse(exchange, "Malformed JWT token");
        } catch (SignatureException e) {
            return unauthorizedResponse(exchange, "Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            return unauthorizedResponse(exchange, "JWT token is empty or invalid");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] responseBytes = ("{\"error\": \"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(responseBytes)));
    }
}
