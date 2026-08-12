package com.educore.sge.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class MockAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String mockRole = request.getHeader("X-User-Role");

        if (mockRole != null && !mockRole.trim().isEmpty()) {
            try {
                // Validamos que pertenezca a los roles del Enum
                Rol rol = Rol.valueOf(mockRole.toUpperCase());

                // Spring Security asume "ROLE_NOMBREDELROL" por defecto
                String authorityName = "ROLE_" + rol.name();
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(authorityName));

                // Inyectamos el usuario simulado en el contexto de seguridad de la petición actual
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "mockUser@educore.com",
                    null,
                    authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (IllegalArgumentException e) {
                // Si el rol enviado no existe, ignoramos y seguimos
                System.out.println("Advertencia: El rol simulado enviado no es válido: " + mockRole);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpieza de seguridad al finalizar el hilo de ejecución
            SecurityContextHolder.clearContext();
        }
    }
}
