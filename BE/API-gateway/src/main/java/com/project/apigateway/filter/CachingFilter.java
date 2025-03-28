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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Component
public class CachingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(CachingFilter.class);

    // List of paths that should not be cached (modify as needed)
    private static final Set<String> NON_CACHEABLE_PATHS = new HashSet<>(Arrays.asList(
            "/users/api/v1/auth/login",
            "/users/api/v1/auth/register",
            "/users/api/v1/auth/logout"
            // Add more non-cacheable paths as needed
    ));

    // HTTP Methods that should not be cached
    private static final Set<HttpMethod> NON_CACHEABLE_METHODS = new HashSet<>(Arrays.asList(
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.PATCH
    ));

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Value("${redis.cache.default-ttl:300}") // Default TTL in seconds (5 minutes)
    private long defaultTtl;

    @Value("${redis.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${redis.cache.timeout:2000}") // Redis operation timeout in milliseconds
    private long redisTimeout;

    @Value("${redis.cache.retry-attempts:3}")
    private int retryAttempts;

    @Value("${redis.cache.retry-delay:100}") // Initial delay in milliseconds
    private long retryDelay;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // Skip caching for certain paths and methods
        if (!cacheEnabled ||
                NON_CACHEABLE_PATHS.contains(path) ||
                NON_CACHEABLE_METHODS.contains(method)) {
            return chain.filter(exchange);
        }

        // Build Cache Key
        String cacheKey = generateCacheKey(exchange);

        // Start with Redis cache lookup with timeout protection
        return redisTemplate.hasKey(cacheKey)
                .timeout(Duration.ofMillis(redisTimeout))
                .onErrorResume(TimeoutException.class, e -> {
                    logger.warn("Redis hasKey operation timed out for key: {}", cacheKey);
                    return Mono.just(false);
                })
                .onErrorResume(e -> {
                    logger.error("Redis hasKey error: {}", e.getMessage());
                    return Mono.just(false);
                })
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return redisTemplate.opsForValue().get(cacheKey)
                                .timeout(Duration.ofMillis(redisTimeout))
                                .onErrorResume(TimeoutException.class, e -> {
                                    logger.warn("Redis get operation timed out for key: {}", cacheKey);
                                    return Mono.empty();
                                })
                                // FIXED: Changed retry(getRetrySpec()) to use withRetry properly
                                .retryWhen(getRetrySpec())
                                .flatMap(cachedResponse -> {
                                    if (cachedResponse != null && !cachedResponse.isEmpty()) {
                                        logger.debug("Cache hit for: {}", cacheKey);
                                        // Cache Hit: return response from cache
                                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                        DataBuffer buffer = exchange.getResponse().bufferFactory()
                                                .wrap(cachedResponse.getBytes(StandardCharsets.UTF_8));
                                        return exchange.getResponse().writeWith(Mono.just(buffer));
                                    }
                                    logger.debug("Empty cache result for: {}", cacheKey);
                                    return processRequest(exchange, chain, cacheKey);
                                })
                                .onErrorResume(e -> {
                                    logger.error("Error retrieving from Redis cache: {}", e.getMessage());
                                    return processRequest(exchange, chain, cacheKey);
                                });
                    }
                    logger.debug("Cache miss for: {}", cacheKey);
                    return processRequest(exchange, chain, cacheKey);
                })
                .onErrorResume(e -> {
                    logger.error("Redis operation error: {}", e.getMessage());
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

                                // Determine TTL based on content or path
                                Duration ttl = determineTtl(exchange, responseBody);

                                // Store in Redis (don't wait for the result and don't break if it fails)
                                redisTemplate.opsForValue().set(cacheKey, responseBody, ttl)
                                        .timeout(Duration.ofMillis(redisTimeout))
                                        // FIXED: Changed retry(getRetrySpec()) to use retryWhen properly
                                        .retryWhen(getRetrySpec())
                                        .subscribe(
                                                success -> logger.debug("Successfully cached: {}", cacheKey),
                                                error -> logger.error("Error saving to Redis cache: {}", error.getMessage())
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
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getQueryParams().isEmpty() ? ""
                : "?" + exchange.getRequest().getQueryParams().toString();

        return "cache:" + method + ":" + path + query;
    }

    // Determine TTL based on content type, path, or other factors
    private Duration determineTtl(ServerWebExchange exchange, String responseBody) {
        String path = exchange.getRequest().getPath().value();

        // Example of custom TTL rules
        if (path.contains("/users/api/v1/profile")) {
            return Duration.ofMinutes(10); // Cache profiles for 10 minutes
        } else if (path.contains("/wallets/api/v1/transactions")) {
            return Duration.ofMinutes(2);  // Cache transactions for 2 minutes
        } else if (responseBody.length() > 10000) {
            return Duration.ofMinutes(10); // Cache large responses longer
        }

        // Default TTL
        return Duration.ofSeconds(defaultTtl);
    }

    // Create a retry specification for Redis operations
    private Retry getRetrySpec() {
        return Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof TimeoutException)) // Don't retry timeouts
                .doBeforeRetry(signal -> logger.debug("Retrying Redis operation after error: {}",
                        signal.failure().getMessage()));
    }
}