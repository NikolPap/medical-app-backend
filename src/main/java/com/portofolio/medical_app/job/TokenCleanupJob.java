package com.portofolio.medical_app.job;

import com.portofolio.medical_app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final VerificationTokenRepository verificationTokenRepository;
    private final Clock clock;

    /**
     *  Läuft jeden Tag um genau 00:00 (Mitternacht)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupTokens() {
        LOGGER.info("Starte nächtlichen Cleanup-Job für abgelaufene Tokens...");

        verificationTokenRepository.deleteExpiredTokens(LocalDateTime.now(clock));

        LOGGER.info("✨ Cleanup-Job erfolgreich beendet.");
    }

}