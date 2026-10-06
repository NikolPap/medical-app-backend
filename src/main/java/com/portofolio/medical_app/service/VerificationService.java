package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.VerificationRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AppUserRepository appUserRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // Προσθέσαμε το Clock εδώ
    private final Clock clock;

    // noRollbackFor: Der Fehlversuch-Zähler muss gespeichert werden, obwohl wir danach eine Exception werfen
    @Transactional(noRollbackFor = AppException.class)
    public void verifyAccount(VerificationRequestDto request) {
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException("Benutzer nicht gefunden."));

        if (user.isEnabled()) {
            throw new AppException("Konto ist bereits verifiziert.");
        }

        VerificationToken verificationToken = verificationTokenRepository.findByUser_EmailAndUsedFalse(user.getEmail())
                .orElseThrow(() -> new AppException("Kein gültiger Bestätigungscode gefunden."));

        // Και το χρησιμοποιούμε εδώ: LocalDateTime.now(clock)
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new AppException("Der Bestätigungscode ist abgelaufen.");
        }

        if (!passwordEncoder.matches(request.code(), verificationToken.getToken())) {
            throw registerFailedAttempt(verificationToken);
        }

        user.setEnabled(true);
        appUserRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    // Erhöht den Zähler und sperrt den Code nach zu vielen Fehlversuchen
    private AppException registerFailedAttempt(VerificationToken token) {
        int attempts = token.getFailedAttempts() + 1;
        token.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            token.setUsed(true); // Code ist ab jetzt ungültig
            verificationTokenRepository.save(token);
            return new AppException("Zu viele Fehlversuche. Der Code wurde ungültig.");
        }

        verificationTokenRepository.save(token);
        return new AppException("Falscher Bestätigungscode! Noch " + (MAX_FAILED_ATTEMPTS - attempts) + " Versuche.");
    }
}