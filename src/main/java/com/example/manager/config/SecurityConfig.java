package com.example.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000", "http://192.168.178.21:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Enable CORS FIRST - before any other configuration
        http.cors(Customizer.withDefaults());
        
        // Disable CSRF for development
        http.csrf(csrf -> csrf.disable());

        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.authorizeHttpRequests(auth -> auth
                // Allow all API endpoints, H2 console, and common routes without authentication
                .requestMatchers("/h2-console/**", "/", "/index.html", "/api/**", "/admin/**", "/frontend/**", "/login", "/logout").permitAll()
                .anyRequest().permitAll() // Allow all other requests for development
        );

        // Disable form login for API - let API handle authentication
        http.formLogin(login -> login.disable());
        http.logout(Customizer.withDefaults());
        
        // Disable default redirect to login
        http.exceptionHandling(handling -> handling.disable());

        return http.build();
    }
}
