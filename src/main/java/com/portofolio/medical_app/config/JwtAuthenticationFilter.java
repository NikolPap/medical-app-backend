package com.portofolio.medical_app.config;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// OncePerRequestFilter garantiert, dass dieser Filter nur EINMAL pro HTTP-Request ausgeführt wird
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwtToken = authHeader.substring(BEARER_PREFIX.length());

        try {
            final String userEmail = jwtService.extractUsername(jwtToken);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                appUserRepository.findByEmail(userEmail)
                        .filter(user -> isUserActive(user) && jwtService.isTokenValid(jwtToken, user))
                        .ifPresent(user -> authenticate(user, request));
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Ungültiger, abgelaufener oder manipulierter Token -> keine Authentifizierung
            LOGGER.debug("JWT konnte nicht verarbeitet werden: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // Nur aktive, nicht gesperrte Konten dürfen ein bestehendes Token weiterverwenden
    private boolean isUserActive(AppUser user) {
        return user.isEnabled() && user.isAccountNonLocked();
    }

    private void authenticate(AppUser user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}