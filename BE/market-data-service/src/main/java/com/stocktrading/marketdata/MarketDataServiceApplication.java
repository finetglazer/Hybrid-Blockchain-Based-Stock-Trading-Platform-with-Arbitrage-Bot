package com.stocktrading.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }

}
