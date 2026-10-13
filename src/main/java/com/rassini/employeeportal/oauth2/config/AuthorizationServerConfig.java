package com.rassini.employeeportal.oauth2.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.rassini.employeeportal.oauth2.service.JpaJwkSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import com.rassini.employeeportal.security.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final JpaJwkSourceService jpaJwkSourceService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final com.rassini.employeeportal.security.JwtAuthenticationFilter jwtAuthFilter;
    private final org.springframework.security.web.context.SecurityContextRepository securityContextRepository;
    private final com.rassini.employeeportal.security.UserSessionRegistry userSessionRegistry;

    @org.springframework.beans.factory.annotation.Value("${app.security.oauth2.authorizationserver.issuer:http://localhost:8083/employee-portal}")
    private String issuer;

    @org.springframework.beans.factory.annotation.Value("${app.security.oauth2.authorizationserver.login-url:http://localhost:4200/employee-portal/auth/login}")
    private String loginUrl;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
            .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
            .with(authorizationServerConfigurer, (authorizationServer) ->
                authorizationServer
                    .oidc(oidc -> oidc
                        .logoutEndpoint(logoutEndpoint -> logoutEndpoint
                            .logoutResponseHandler((request, response, authentication) -> {

                                // ── 1. Invalidar sesión por username (fix principal) ──────────────
                                String targetUsername = null;
                                if (authentication != null && authentication.getName() != null
                                        && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                                    targetUsername = authentication.getName();
                                }
                                if (targetUsername == null) {
                                    org.springframework.security.core.Authentication currentAuth =
                                            org.springframework.security.core.context.SecurityContextHolder
                                                    .getContext().getAuthentication();
                                    if (currentAuth != null && currentAuth.isAuthenticated()
                                            && !(currentAuth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                                        targetUsername = currentAuth.getName();
                                    }
                                }
                                if (targetUsername == null) {
                                    String idTokenHint = request.getParameter("id_token_hint");
                                    if (idTokenHint != null && idTokenHint.contains(".")) {
                                        try {
                                            String[] parts = idTokenHint.split("\\.");
                                            if (parts.length >= 2) {
                                                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                                                // Simple extraction of "sub":"..."
                                                int subIdx = payload.indexOf("\"sub\":\"");
                                                if (subIdx != -1) {
                                                    int start = subIdx + 7;
                                                    int end = payload.indexOf("\"", start);
                                                    if (end != -1) {
                                                        targetUsername = payload.substring(start, end);
                                                    }
                                                }
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }

                                if (targetUsername != null) {
                                    userSessionRegistry.invalidateSession(targetUsername);
                                }

                                // ── 2. Invalidar también sesión de la request actual (si existe) ──
                                jakarta.servlet.http.HttpSession session = request.getSession(false);
                                if (session != null) {
                                    session.invalidate();
                                }

                                // ── 3. Limpiar SecurityContext ────────────────────────────────────
                                org.springframework.security.core.context.SecurityContextHolder.clearContext();

                                // ── 4. Eliminar cookies JSESSIONID ────────────────────────────────
                                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
                                cookie.setPath(request.getContextPath() != null && !request.getContextPath().isEmpty()
                                        ? request.getContextPath() : "/employee-portal");
                                cookie.setMaxAge(0);
                                cookie.setHttpOnly(true);
                                response.addCookie(cookie);

                                jakarta.servlet.http.Cookie rootCookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
                                rootCookie.setPath("/");
                                rootCookie.setMaxAge(0);
                                rootCookie.setHttpOnly(true);
                                response.addCookie(rootCookie);

                                // ── 5. Redirigir al Portal con marcador oidc_logout=1 ─────────────
                                String postLogoutRedirectUri = request.getParameter("post_logout_redirect_uri");
                                String redirectTarget = (postLogoutRedirectUri != null && !postLogoutRedirectUri.isBlank())
                                        ? postLogoutRedirectUri
                                        : this.loginUrl;
                                String separator = redirectTarget.contains("?") ? "&" : "?";
                                response.sendRedirect(redirectTarget + separator + "oidc_logout=1");
                            })
                        )
                    )
            )
            .authorizeHttpRequests((authorize) ->
                authorize.anyRequest().authenticated()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .exceptionHandling((exceptions) -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(this.loginUrl),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jpaJwkSourceService);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(this.issuer)
                .build();
    }
}
