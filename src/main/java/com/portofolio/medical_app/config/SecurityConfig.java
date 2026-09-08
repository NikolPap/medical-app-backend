package com.portofolio.medical_app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    //1. Der Passwort-Vershlüsseler (BCrypt ist der Industrie-Standard)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //2. Die Sicherheitsregeln (Wer darf was?)
    @SuppressWarnings("squid:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        try {
            http
                    // SonarQube Fix 2: Method Reference statt Lambda
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .csrf(AbstractHttpConfigurer::disable)
                    .headers(headers -> headers.frameOptions(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig::disable))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/appointments/available-slots").permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

            return http.build();

        } catch (Exception e) {
            // SonarQube Fix 3: Wir fangen mögliche Setup-Fehler ab und werfen unsere EIGENE Custom Exception
            throw new com.portofolio.medical_app.exception.AppException("Fehler bei der Security-Konfiguration: " + e.getMessage(), e);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Wer darf mit uns sprechen? (Die Angular App!)
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // Welche Methoden sind erlaubt?
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Welche Header dürfen mitgeschickt werden? (WICHTIG FÜR JWT!)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Dürfen Cookies/Credentials gesendet werden?
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Gilt für ALLE Endpoints
        return source;
    }
}
