package com.stocktrading.orderservice.config;

import com.stocktrading.orderservice.handler.OrderWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@RequiredArgsConstructor
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket handler with CORS support
        registry.addHandler(orderWebSocketHandler, "/ws/orders")
                .setAllowedOrigins("http://127.0.0.1:5173", "http://localhost:5173");
    }


    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192 * 10); // Increase buffer size
        container.setMaxBinaryMessageBufferSize(8192 * 10);
        container.setMaxSessionIdleTimeout(60000L);
        return container;
    }

}
