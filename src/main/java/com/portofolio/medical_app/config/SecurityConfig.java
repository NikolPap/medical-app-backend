package com.portofolio.medical_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    //1. Der Passwort-Vershlüsseler (BCrypt ist der Industrie-Standard)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //2. Die Sicherheitsregeln (Wer darf was?)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())//Deaktiviert CSRF(wichtig für Rest-Apis, sonst werden POst-Anfragen blockiert)
                .authorizeHttpRequests(auth -> auth
                //Diese URLs dürfen von JEDEM aufgerufen werden (z.B. registrierung / Login)
                        .requestMatchers("/api/auth/**").permitAll()
                        //Für Alle anderen URLs muss man eingeloggt sein!
                        .anyRequest().authenticated());
        return http.build();
    }

}
