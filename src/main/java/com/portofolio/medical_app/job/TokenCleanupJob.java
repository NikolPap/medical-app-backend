package com.portofolio.medical_app.job;

import com.portofolio.medical_app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {
    private final VerificationTokenRepository verificationTokenRepository;
    //Laüft jeden Tag um genau 00:00 (Mitternacht)
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupTokens() {
        log.info("Starte nächtlichen Cleanup-Job für abgelaufene Tokens...");

        verificationTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        log.info("✨ Cleanup-Job erfolgreich beendet.");
    }
}
