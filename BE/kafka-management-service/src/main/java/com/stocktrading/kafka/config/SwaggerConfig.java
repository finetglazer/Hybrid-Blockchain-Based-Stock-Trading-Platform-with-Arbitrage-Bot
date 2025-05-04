package com.stocktrading.kafka.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for Swagger/OpenAPI documentation
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Kafka Management Service API")
                .description("API for managing Kafka messages and sagas in the Stock Trading Platform")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Stock Trading Platform Team")
                    .email("contact@stocktradingplatform.com"))
                .license(new License()
                    .name("Private")))
            .servers(List.of(
                new Server().url("http://localhost:8085").description("Local Development Server")
            ));
    }
}
