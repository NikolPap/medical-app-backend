package com.portofolio.medical_app.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // ΠΡΟΣΤΕΘΗΚΕ ΤΟ IMPORT
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    // 1. Der Passwort-Verschlüsseler (BCrypt ist der Industrie-Standard)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Die Sicherheitsregeln (Wer darf was?)
    @SuppressWarnings("squid:S4502") // CSRF wird absichtlich deaktiviert (Stateless JWT Architektur)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        try {
            http
                    // CORS aktivieren, damit Angular mit uns sprechen darf
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                    // SonarQube Fix 2: Method Reference statt Lambda
                    .csrf(AbstractHttpConfigurer::disable)

                    // Stateless Session Management (Wichtig für JWT)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                    // Endpunkte konfigurieren
                    .authorizeHttpRequests(auth -> auth
                            // Diese URLs dürfen von JEDEM aufgerufen werden (z.B. Registrierung / Login / Freie Slots)
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/appointments/available-slots").permitAll()
                            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                            .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                            .anyRequest().authenticated()
                    )
                    // Exception Handling για 401 Unauthorized
                    .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"message\":\"Bitte melde dich an.\"}");
                    }))
                    // Unseren JWT Filter an die Tür stellen!
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();

        } catch (Exception e) {
            // SonarQube Fix 3: Wir fangen mögliche Setup-Fehler ab und werfen unsere EIGENE Custom Exception
            throw new com.portofolio.medical_app.exception.AppException("Fehler bei der Security-Konfiguration: " + e.getMessage(), e);
        }
    }

    // 3. CORS Konfiguration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));

        // Welche Methoden sind erlaubt?
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Gilt für ALLE Endpoints
        return source;
    }
}