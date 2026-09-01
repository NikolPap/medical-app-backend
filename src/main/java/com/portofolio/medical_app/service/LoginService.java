package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.AuthResponseDto;
import com.portofolio.medical_app.dto.LoginRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDto login(LoginRequestDto request) {

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Falsche E-Mail oder Passwort."));

        // 1. Hat er seine E-Mail verifiziert?
        if (!user.isEnabled()) {
            throw new RuntimeException("Bitte bestätige zuerst deine E-Mail-Adresse!");
        }

        // 2. Ist der Account gesperrt (weil er 3x falsch lag)?
        if (!user.isAccountNonLocked()) {
            if (unlockWhenTimeExpired(user)) {
                // Die 15 Minuten sind vorbei! Konto wurde gerade wieder entsperrt.
            } else {
                throw new RuntimeException("Konto ist vorübergehend gesperrt. Bitte warte 15 Minuten.");
            }
        }

        // 3. STIMMT DAS PASSWORT?
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {

            // PASSWORT FALSCH -> Fehlversuche erhöhen!
            registerFailedLogin(user);
            throw new RuntimeException("Falsche E-Mail oder Passwort.");
        }

        // 4. PASSWORT RICHTIG! -> Fehlversuche auf 0 zurücksetzen
        resetFailedAttempts(user);

        // 5. Token generieren und zurückgeben
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponseDto(jwtToken, user.getRole());
    }

    // --- HILFSMETHODEN FÜR DAS ACCOUNT LOCKING ---

    private void registerFailedLogin(AppUser user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            // Konto sperren!
            user.setAccountNonLocked(false);
            user.setLockTime(LocalDateTime.now());
        }
        appUserRepository.save(user); // Änderungen in der DB speichern
    }

    private void resetFailedAttempts(AppUser user) {
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockTime(null);
        appUserRepository.save(user);
    }

    private boolean unlockWhenTimeExpired(AppUser user) {
        LocalDateTime lockTime = user.getLockTime();
        if (lockTime != null && lockTime.plusMinutes(LOCK_TIME_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedLoginAttempts(0);
            appUserRepository.save(user);
            return true;
        }
        return false;
    }
}
