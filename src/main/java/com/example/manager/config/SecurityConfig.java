package com.example.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // For development: allow the H2 console and frames, relax CSRF for the console
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**", "/admin/**"));

        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/", "/index.html", "/api/**", "/frontend/**", "/login").permitAll()
                .anyRequest().authenticated()
        );

        http.formLogin(login -> login.defaultSuccessUrl("/", true).permitAll());
        http.logout(Customizer.withDefaults());

        return http.build();
    }
}
