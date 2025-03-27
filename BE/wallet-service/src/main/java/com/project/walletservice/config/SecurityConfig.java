package com.project.walletservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    // 1) Provide a BCryptPasswordEncoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        // The integer parameter is the "strength" or cost factor (default is usually 10)
        return new BCryptPasswordEncoder(10);
    }

    // 2) Define a SecurityFilterChain using the new (Spring Security 6+) configuration approach
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF
                .authorizeHttpRequests(auth -> auth
                        // Permit Swagger and API documentation access

                        // Require authentication for other endpoints
                        .anyRequest().permitAll()
                );
        // Build and return the SecurityFilterChain
        return http.build();
    }
}
