package com.educore.sge.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InstitutionWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Buscamos el ID del colegio en la petición (Ej: X-Institution-Id: 1234-5678-...)
        String institutionId = request.getHeader("X-Institution-Id");

        if (institutionId != null) {
            // Lo guardamos en nuestra memoria a corto plazo
            InstitutionContext.setCurrentInstitution(institutionId);
        }

        try {
            // Dejamos que la petición siga su camino hacia el Controller
            filterChain.doFilter(request, response);
        } finally {
            // ¡MUY IMPORTANTE! Limpiamos la memoria al terminar para que no se filtren datos a otras peticiones
            InstitutionContext.clear();
        }
    }
}
