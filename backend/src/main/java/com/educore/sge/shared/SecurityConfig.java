package com.educore.sge.shared;

import com.educore.sge.auth.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthAccountRepository accounts, AuthSessionRepository sessions, JwtTokenService jwtTokens) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/institutions", "/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        // El tutor solo puede gestionar su sesión y consultar sus estudiantes vinculados.
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/v1/**").hasAnyRole("OWNER","DIRECTOR","ADMINISTRATIVE","PRECEPTOR","TEACHER")
                        .anyRequest().authenticated())
                .addFilterBefore(new BearerAuthFilter(accounts,sessions,jwtTokens), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401, "Debe iniciar sesión")));
        return http.build();
    }
    @Value("${educore.allowed-origins:http://localhost:4200,http://127.0.0.1:4200,http://localhost:5500,http://127.0.0.1:5500}")
    private String origins;    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(x->!x.isBlank()).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Institution-Id"));
        c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",c);
        return source;
    }
}
