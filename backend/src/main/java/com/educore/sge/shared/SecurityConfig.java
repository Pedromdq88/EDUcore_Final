package com.educore.sge.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 👈 Habilita CORS explícitamente
                .csrf(csrf -> csrf.disable()) // Deshabilitado para APIs REST stateless
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // O las reglas que tengas configuradas
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Permite peticiones desde el puerto de desarrollo de WebStorm / LiveServer
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Permite todos los métodos HTTP requeridos (GET, POST, PUT, DELETE, OPTIONS)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Permite las cabeceras personalizadas que enviamos desde app.js (X-Institution-Id, X-User-Role, etc.)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Institution-Id", "X-User-Role"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}