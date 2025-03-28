package com.project.apigateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class CachingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(CachingFilter.class);

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Build Cache Key
        String cacheKey = generateCacheKey(exchange);

        return redisTemplate.hasKey(cacheKey)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return redisTemplate.opsForValue().get(cacheKey)
                                .flatMap(cachedResponse -> {
                                    if (cachedResponse != null && !cachedResponse.isEmpty()) {
                                        // Cache Hit: return response from cache
                                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(cachedResponse.getBytes(StandardCharsets.UTF_8));
                                        return exchange.getResponse().writeWith(Mono.just(buffer));
                                    }
                                    return processRequest(exchange, chain, cacheKey);
                                })
                                .onErrorResume(e -> {
                                    logger.error("Error retrieving from Redis cache: {}", e.getMessage());
                                    return processRequest(exchange, chain, cacheKey);
                                });
                    }
                    return processRequest(exchange, chain, cacheKey);
                })
                .onErrorResume(e -> {
                    logger.error("Redis connection error: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> processRequest(ServerWebExchange exchange, WebFilterChain chain, String cacheKey) {
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (getStatusCode() == HttpStatus.OK) {
                    return DataBufferUtils.join(body)
                            .flatMap(dataBuffer -> {
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);

                                String responseBody = new String(content, StandardCharsets.UTF_8);

                                // Store in Redis (don't wait for the result and don't break if it fails)
                                redisTemplate.opsForValue().set(cacheKey, responseBody, Duration.ofMinutes(5))
                                        .subscribe(null, error ->
                                                logger.error("Error saving to Redis cache: {}", error.getMessage())
                                        );

                                // Return response to client
                                DataBuffer buffer = getDelegate().bufferFactory().wrap(content);
                                return super.writeWith(Mono.just(buffer));
                            });
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(responseDecorator).build());
    }

    // Helper to create unique keys
    private String generateCacheKey(ServerWebExchange exchange) {
        String query = exchange.getRequest().getQueryParams().isEmpty() ? "" : "?" + exchange.getRequest().getQueryParams();
        return "cache:" + exchange.getRequest().getMethod() + ":" + exchange.getRequest().getURI() + query;
    }
}
