package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.ForgotPasswordRequestDto;
import com.portofolio.medical_app.dto.ResetPasswordRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.PasswordResetToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.PasswordResetTokenMapper; // 1. IMPORT
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenMapper passwordResetTokenMapper; // 2. O MAPPER ΜΑΣ

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequestDto request) {

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException("Benutzer nicht gefunden."));

        if(!user.isEnabled()) {
            throw new AppException("Bitte bestätige zuerst deine E-Mail-Adresse!");
        }

        // 3. ΣΗΜΕΙΩΣΗ: Αφαίρεσα το `new SecureRandom()` που είχες εδώ,
        // γιατί ήδη έχουμε το secureRandom πιο πάνω!
        String randomCode = String.format("%06d", secureRandom.nextInt(999999));
        String hashedCode = passwordEncoder.encode(randomCode);

        // 4. ΧΡΗΣΗ ΤΟΥ MAPPER! Ο κώδικας έγινε πανέμορφος.
        PasswordResetToken resetToken = passwordResetTokenMapper.toEntity(hashedCode, user);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), randomCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException("Benutzer nicht gefunden."));

        PasswordResetToken resetToken = passwordResetTokenRepository.findValidTokenByEmail(user.getEmail())
                .orElseThrow(() -> new AppException("Kein gültiger Reset-Code gefunden."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("Der Code ist abgelaufen. Bitte fordere einen neuen an.");
        }

        if (!passwordEncoder.matches(request.code(), resetToken.getToken())) {
            throw new AppException("Falscher Bestätigungscode!");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        appUserRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // NEU: Prüft nur, ob der Code korrekt ist (für Step 1 im Frontend)
    public void verifyResetCode(String email, String code) {

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Benutzer nicht gefunden."));

        PasswordResetToken resetToken = passwordResetTokenRepository.findValidTokenByEmail(user.getEmail())
                .orElseThrow(() -> new AppException("Kein gültiger Reset-Code gefunden."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("Der Code ist abgelaufen. Bitte fordere einen neuen an.");
        }

        if (!passwordEncoder.matches(code, resetToken.getToken())) {
            throw new AppException("Falscher Bestätigungscode!");
        }

        // Wenn alles stimmt, werfen wir keine Exception! Der Controller meldet "200 OK".
    }

}