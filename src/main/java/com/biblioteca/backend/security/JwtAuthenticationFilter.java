package com.biblioteca.backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// Recibe la petición REST, si trae un token se lo pasa a JwtService, si es autorizado enruta al controller.

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Token";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(HEADER_NAME);

        if (token != null && !token.isBlank()) {
            try {
                String uid = jwtService.extraerUid(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(uid, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException e) {
                // Token inválido o expirado: no autenticamos, dejamos pasar sin contexto de seguridad.
                // Spring Security rechazará el request más adelante si el endpoint requiere autenticación.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}