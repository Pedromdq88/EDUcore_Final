package com.educore.sge.auth.security;

import com.educore.sge.auth.AuthService;
import com.educore.sge.auth.SessionAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthService auth) throws Exception {
        http.headers(headers -> headers
                    .contentTypeOptions(options -> {})
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers("/api/v1/public/**", "/api/v1/auth/login", "/api/v1/auth/forgot-password", "/api/v1/auth/verify-reset-code", "/api/v1/auth/reset-password", "/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/api/calendar/**").denyAll() // Endpoint antiguo no aislado por institución.
                        .requestMatchers("/api/v1/auth/my-students", "/api/v1/auth/my-students/**").hasRole("TUTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/announcements/students/*/private")
                            .hasAnyRole("DIRECTOR", "ADMINISTRATIVE", "PRECEPTOR", "TUTOR")
                        // Lectura limitada. Cada controlador comprueba la institución y los alumnos vinculados.
                        .requestMatchers(HttpMethod.GET, "/api/v1/announcements", "/api/v1/calendar/events",
                                "/api/v1/institution/profile", "/api/v1/institutions/settings/emails",
                                "/api/v1/classrooms-config", "/api/v1/class-schedules",
                                "/api/v1/students/*/fees", "/api/v1/students/*/medical-record",
                                "/api/v1/students/*/authorized-pickups").hasAnyRole("DIRECTOR","ADMINISTRATIVE","PRECEPTOR","TEACHER","TUTOR")
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        // El perfil TUTOR no puede consultar listados generales ni legajos de otras familias.
                        .requestMatchers("/api/v1/**").hasAnyRole("OWNER", "DIRECTOR", "ADMINISTRATIVE", "PRECEPTOR", "TEACHER")
                        .anyRequest().authenticated())
                .addFilterBefore(new SessionAuthFilter(auth), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Institution-Id"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
