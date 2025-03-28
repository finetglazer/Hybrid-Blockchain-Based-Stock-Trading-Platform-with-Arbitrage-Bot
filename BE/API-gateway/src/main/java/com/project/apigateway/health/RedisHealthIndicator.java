package com.project.apigateway.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);

    @Autowired
    private ReactiveRedisConnectionFactory connectionFactory;

    @Override
    public Mono<Health> health() {
        return connectionFactory.getReactiveConnection()
                .ping()
                .timeout(Duration.ofSeconds(3))
                .map(ping -> {
                    logger.debug("Redis health check: {}", ping);
                    return Health.up()
                            .withDetail("ping", ping)
                            .withDetail("version", "Redis")
                            .build();
                })
                .onErrorResume(throwable -> {
                    logger.error("Redis health check failed: {}", throwable.getMessage());
                    return Mono.just(Health.down()
                            .withDetail("error", throwable.getMessage())
                            .build());
                });
    }
}