package com.stocktrading.marketdata.config;

import org.springframework.context.annotation.Bean;
import com.stocktrading.marketdata.handler.StockDataWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.stocktrading.marketdata.websocket.MarketDataWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@RequiredArgsConstructor
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MarketDataWebSocketHandler marketDataWebSocketHandler;  // Use Autowired instead
    private final StockDataWebSocketHandler stockDataWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket handler with CORS support
        registry.addHandler(marketDataWebSocketHandler, "/ws/market-data")
                .setAllowedOrigins("*"); // In production, specify exact origins
        registry.addHandler(stockDataWebSocketHandler, "/market-data/ws/stock-data")
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