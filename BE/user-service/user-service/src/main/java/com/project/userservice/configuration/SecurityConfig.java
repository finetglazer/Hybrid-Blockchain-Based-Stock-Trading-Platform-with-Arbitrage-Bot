package com.project.userservice.configuration;

import com.project.userservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // 1) Provide a BCryptPasswordEncoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        // The integer parameter is the "strength" or cost factor (default is usually 10)
        return new BCryptPasswordEncoder(10);
    }

    // 2) Define a SecurityFilterChain using the new (Spring Security 6+) configuration approach
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Configure Security:
        http
                // (A) Disable CSRF for simplicity in a stateless REST context
                .csrf(csrf -> csrf.disable())
                // (B) Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Permit all requests to /register and /verify (our public endpoints)
                        .requestMatchers("/api/v1/auth/register","api/v1/auth/verify", "api/v1/auth/login", "api/v1/auth/forgot-password", "api/v1/auth/reset-password").permitAll()
                        // All other endpoints require authentication by default
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        // Build and return the SecurityFilterChain
        return http.build();
    }
}
