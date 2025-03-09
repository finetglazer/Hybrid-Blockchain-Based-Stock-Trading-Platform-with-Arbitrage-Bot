//package com.project.apigateway.filter;
//
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//
//
//@Component
//public class CachingFilter implements WebFilter {
//
//    @Autowired
//    private ReactiveStringRedisTemplate redisTemplate;
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        // Build Cache Key
//        String cacheKey = generateCacheKey(exchange);
//        return redisTemplate.opsForValue()
//                .get(cacheKey)
//                .flatMap(cachedResponse -> {
//                    if (cachedResponse != null) {
//                        // Cache Hit: return response from cache
//                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
//                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(cachedResponse.getBytes());
//                        return exchange.getResponse().writeWith(Mono.just(buffer));
//                    }
//
//                    // Cache Miss: proceed with the next filter in chain
//                    return chain.filter(exchange).doOnTerminate(() -> {
//                        // Cache the response if it's successful
//                        if (exchange.getResponse().getStatusCode().equals(HttpStatus.OK)) {
//                            String responseBody = "Captured Response Body"; // Capture body from exchange (see next steps)
//                            redisTemplate.opsForValue().set(cacheKey, responseBody, Duration.ofMinutes(5)).subscribe();
//                        }
//                    });
//                });
//    }
//
//    // Helper to create unique keys
//    private String generateCacheKey(ServerWebExchange exchange) {
//        String query = exchange.getRequest().getQueryParams().isEmpty() ? "" : "?" + exchange.getRequest().getQueryParams();
//        return "cache:" + exchange.getRequest().getMethod() + ":" + exchange.getRequest().getURI() + query;
//    }
//}
//
