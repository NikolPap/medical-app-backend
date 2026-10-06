package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.ForgotPasswordRequestDto;
import com.portofolio.medical_app.dto.ResetPasswordRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.PasswordResetToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.PasswordResetTokenMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenMapper passwordResetTokenMapper;

    // ΕΔΩ ΜΠΗΚΕ ΤΟ CLOCK, ΜΕΣΑ ΣΤΗΝ ΚΛΑΣΗ!
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequestDto request) {

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException("Benutzer nicht gefunden."));

        if (!user.isEnabled()) {
            throw new AppException("Bitte bestätige zuerst deine E-Mail-Adresse!");
        }

        // Es darf immer nur EIN gültiger Code existieren
        passwordResetTokenRepository.invalidateAllActiveTokens(user.getId());

        String randomCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hashedCode = passwordEncoder.encode(randomCode);

        PasswordResetToken resetToken = passwordResetTokenMapper.toEntity(hashedCode, user);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), randomCode);
    }

    @Transactional(noRollbackFor = AppException.class)
    public void resetPassword(ResetPasswordRequestDto request) {

        PasswordResetToken resetToken = validateCode(request.email(), request.code());

        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        appUserRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // Prüft nur, ob der Code korrekt ist (Schritt 1 im Frontend)
    @Transactional(noRollbackFor = AppException.class)
    public void verifyResetCode(String email, String code) {
        validateCode(email, code);
    }

    // Gemeinsame Prüfung: Code vorhanden, nicht abgelaufen, korrekt, Versuche nicht überschritten
    private PasswordResetToken validateCode(String email, String code) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findValidTokenByEmail(email)
                .orElseThrow(() -> new AppException("Kein gültiger Reset-Code gefunden."));

        // ΕΔΩ ΕΓΙΝΕ Η ΑΛΛΑΓΗ ΣΕ LocalDateTime.now(clock)
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new AppException("Der Code ist abgelaufen. Bitte fordere einen neuen an.");
        }

        if (!passwordEncoder.matches(code, resetToken.getToken())) {
            throw registerFailedAttempt(resetToken);
        }

        return resetToken;
    }

    private AppException registerFailedAttempt(PasswordResetToken token) {
        int attempts = token.getFailedAttempts() + 1;
        token.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            return new AppException("Zu viele Fehlversuche. Bitte fordere einen neuen Code an.");
        }

        passwordResetTokenRepository.save(token);
        return new AppException("Falscher Bestätigungscode! Noch " + (MAX_FAILED_ATTEMPTS - attempts) + " Versuche.");
    }
}