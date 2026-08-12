package com.educore.sge.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita las anotaciones @PreAuthorize y @PostAuthorize
public class SecurityConfig {

    private final MockAuthFilter mockAuthFilter;

    public SecurityConfig(MockAuthFilter mockAuthFilter) {
        this.mockAuthFilter = mockAuthFilter;
    }

    /**
     * Define la jerarquía de roles de EDUcore utilizando la API de Spring Security 6.
     * Esto evita repetir roles redundantes en los controladores.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        // En Spring Security 6 (Spring Boot 3.x), se utiliza el método estático fromHierarchy
        return RoleHierarchyImpl.fromHierarchy(
            "ROLE_OWNER > ROLE_DIRECTOR\n" +
                "ROLE_DIRECTOR > ROLE_ADMINISTRATIVE\n" +
                "ROLE_ADMINISTRATIVE > ROLE_TEACHER"
        );
    }

    /**
     * Vincula la jerarquía de roles con el motor de seguridad por métodos (@PreAuthorize)
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        // Agregamos nuestro filtro de simulación de desarrollo antes del filtro estándar
        http.addFilterBefore(mockAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
