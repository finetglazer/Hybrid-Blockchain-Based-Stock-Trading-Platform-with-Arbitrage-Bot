package com.stocktrading.brokerage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MockBrokerageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockBrokerageServiceApplication.class, args);
    }

}
