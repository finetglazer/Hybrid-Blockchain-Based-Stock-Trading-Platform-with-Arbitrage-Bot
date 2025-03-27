//package com.project.apigateway.filter;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//@Component
//@Order(-200) // high precedence (before security filter)
//public class LoggingWebFilter implements WebFilter {
//
//    private static final Logger logger = LoggerFactory.getLogger(LoggingWebFilter.class);
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        long startTime = System.currentTimeMillis();
//
//        return chain.filter(exchange).doOnTerminate(() -> {
//            long duration = System.currentTimeMillis() - startTime;
//
//            String method = exchange.getRequest().getMethod().name();
//            String path = exchange.getRequest().getURI().getPath();
//            String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
//            int statusCode = exchange.getResponse().getStatusCode() != null ?
//                    exchange.getResponse().getStatusCode().value() : 0;
//
//            logger.info("{} {} | IP: {} | Status: {} | Time: {}ms",
//                    method, path, clientIp, statusCode, duration);
//        });
//    }
//}
