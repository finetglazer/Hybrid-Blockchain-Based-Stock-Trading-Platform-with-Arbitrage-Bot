package com.stocktrading.marketdata.config;

import com.stocktrading.marketdata.handler.StockDataWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@RequiredArgsConstructor
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StockDataWebSocketHandler stockDataWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stockDataWebSocketHandler, "/market-data/ws/stock-data")
                .setAllowedOrigins("http://127.0.0.1:5173", "http://localhost:5173");
    }
}
