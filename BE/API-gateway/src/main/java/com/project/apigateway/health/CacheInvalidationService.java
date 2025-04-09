package com.project.apigateway.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Service
public class CacheInvalidationService {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationService.class);

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    /**
     * Invalidate specific cache keys
     * @param keys Cache keys to invalidate
     * @return Mono<Long> Number of keys deleted
     */
    public Mono<Long> invalidateKeys(String... keys) {
        if (keys.length == 0) {
            return Mono.just(0L);
        }

        logger.debug("Invalidating {} cache keys: {}", keys.length, Arrays.toString(keys));
        // Fixed: Properly create a Flux from the keys array
        return redisTemplate.delete(Flux.fromArray(keys))
                .doOnSuccess(count -> logger.debug("Successfully invalidated {} cache keys", count))
                .doOnError(e -> logger.error("Error invalidating cache keys: {}", e.getMessage()));
    }

    /**
     * Invalidate all cache entries with a specific pattern
     * @param pattern Key pattern to match (e.g., "cache:GET:/users/*")
     * @return Mono<Long> Number of keys deleted
     */
    public Mono<Long> invalidatePattern(String pattern) {
        logger.debug("Invalidating cache keys matching pattern: {}", pattern);
        return redisTemplate.keys(pattern)
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        logger.debug("No keys found matching pattern: {}", pattern);
                        return Mono.just(0L);
                    }

                    logger.debug("Found {} keys matching pattern: {}", keys.size(), pattern);
                    // Fixed: Properly create a Flux from the keys list
                    return redisTemplate.delete(Flux.fromIterable(keys));
                })
                .doOnSuccess(count -> logger.debug("Successfully invalidated {} cache keys", count))
                .doOnError(e -> logger.error("Error invalidating cache keys: {}", e.getMessage()));
    }

    /**
     * Invalidate cache based on a request
     * This is useful for invalidating cache when data changes (POST/PUT/DELETE operations)
     */
    public Mono<Long> invalidateForRequest(ServerHttpRequest request) {
        String path = request.getPath().value();

        // Define path mapping for cache invalidation
        // When a path is modified, invalidate related GET caches

        // For example, if /users/123 is updated, invalidate all GET requests for /users/*
        if (path.matches("/users/api/v1/\\d+(/.*)?")) {
            return invalidatePattern("cache:GET:/users/api/v1/*");
        }

        // If a wallet transaction is created or modified, invalidate all wallet transaction caches
        if (path.matches("/wallets/api/v1/transactions(/.*)?")) {
            return invalidatePattern("cache:GET:/wallets/api/v1/transactions*");
        }

        // Add more specific invalidation rules as needed

        // Fallback: invalidate exact path
        return invalidateKeys("cache:GET:" + path);
    }

    /**
     * Invalidate all cache entries
     * Use with caution!
     */
    public Mono<Long> invalidateAll() {
        logger.warn("Invalidating ALL cache entries - this should be used with caution");
        return redisTemplate.keys("cache:*")
                .collectList()
                .flatMap(keys -> {
                    if (keys.isEmpty()) {
                        return Mono.just(0L);
                    }
                    // Fixed: Properly create a Flux from the keys list
                    return redisTemplate.delete(Flux.fromIterable(keys));
                })
                .doOnSuccess(count -> logger.debug("Successfully invalidated all {} cache entries", count))
                .doOnError(e -> logger.error("Error invalidating all cache entries: {}", e.getMessage()));
    }
}