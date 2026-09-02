package com.portofolio.medical_app.config;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.Nonnull;
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

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Wir suchen im Header des Requests nach dem Wort "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String userEmail;

        // Wenn der Header fehlt oder nicht mit "Bearer " beginnt, überspringen wir diesen Filter.
        // (Das passiert z. B. beim Login oder bei der Registrierung, wo man noch kein Token hat)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Wir schneiden das Wort "Bearer " (7 Zeichen) ab, um das reine Token zu bekommen
        jwtToken = authHeader.substring(7);

        try {
            // 3. E-Mail aus dem Token extrahieren
            userEmail = jwtService.extractUsername(jwtToken);

            // 4. Wenn eine E-Mail vorhanden ist UND der Benutzer in diesem Request noch nicht authentifiziert wurde
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Wir holen den Benutzer aus der Datenbank
                AppUser user = appUserRepository.findByEmail(userEmail).orElse(null);

                // 5. Wenn der Benutzer existiert und das Token gültig ist
                if (user != null && jwtService.isTokenValid(jwtToken, user)) {

                    // 6. Wir sagen Spring Security: "Das Token ist echt! Lass ihn rein!"
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null, // Passwort wird nicht benötigt, das Token ist Beweis genug
                            user.getAuthorities() // Übergibt die Rolle (z.B. ROLE_DOCTOR)
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Wir speichern die Authentifizierung im Security-Kontext (Speicher für diesen Request)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Wenn das Token ungültig oder abgelaufen ist, fangen wir den Fehler ab.
            // Der Benutzer wird später von Spring Security automatisch blockiert (403 Forbidden).
        }

        // 7. Gehe weiter zum nächsten Filter (oder zum Controller)
        filterChain.doFilter(request, response);
    }
}