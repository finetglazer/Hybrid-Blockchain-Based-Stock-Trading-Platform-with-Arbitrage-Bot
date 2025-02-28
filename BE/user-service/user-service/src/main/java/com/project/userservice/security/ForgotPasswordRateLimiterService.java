package com.project.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ForgotPasswordRateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String FORGOT_PASSWORD_PREFIX = "forgotPassword:";

    // For example: allow a maximum of 3 requests per email per hour
    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_SECONDS = 3600; // 1 hour

    public boolean isAllowed(String email) {
        String key = FORGOT_PASSWORD_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(WINDOW_SECONDS));
            return true;
        } else {
            int count = Integer.parseInt(value);
            if (count < MAX_REQUESTS) {
                redisTemplate.opsForValue().increment(key);
                return true;
            } else {
                return false;
            }
        }
    }
}
