package com.rassini.employeeportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        // Validar si el header existe y empieza con Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token
        jwt = authHeader.substring(7);
        
        // 1. Intentar validar como HMAC (token de login interno tradicional)
        boolean authenticated = false;
        try {
            io.jsonwebtoken.Claims claims = jwtService.extractAllClaims(jwt);
            String tokenType = claims.get("token_type", String.class);
            Boolean mfaPending = claims.get("mfa_pending", Boolean.class);

            if ("MFA_PENDING".equals(tokenType) || Boolean.TRUE.equals(mfaPending)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso Denegado: Los tokens MFA_PENDING no pueden usarse como Bearer Token.");
                return;
            }

            String username = claims.getSubject();
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    authenticated = true;
                }
            }
        } catch (Exception e) {
            // No es HMAC o falló la validación HMAC. Continuamos para intentar decodificar como OAuth2 RS256 JWT
            log.debug("[JwtAuthenticationFilter] HMAC decoding attempt bypassed/failed: {}", e.getMessage());
        }

        // 2. Si no se autenticó por HMAC, intentar validar como RS256 OAuth2 Token de Spring Authorization Server
        if (!authenticated && SecurityContextHolder.getContext().getAuthentication() == null) {
            JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
            if (jwtDecoder != null) {
                try {
                    Jwt decodedJwt = jwtDecoder.decode(jwt);
                    String username = decodedJwt.getSubject();
                    if (username != null) {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        authenticated = true;
                        log.debug("[JwtAuthenticationFilter] Successfully authenticated OAuth2 RS256 token for user {}", username);
                    }
                } catch (Exception e) {
                    log.warn("[JwtAuthenticationFilter] Error validando OAuth2 RS256 token en {} {}: {} - {}",
                            request.getMethod(), request.getRequestURI(),
                            e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
