package com.portofolio.medical_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    // Alle Termine sind lokale deutsche Zeiten, deshalb feste Zeitzone statt Server-Standard
    // (in Docker/Cloud läuft der Server oft in UTC).
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Berlin"));
    }
}