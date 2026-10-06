package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.AuthResponseDto;
import com.portofolio.medical_app.dto.LoginRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String MSG_BAD_CREDENTIALS = "Falsche E-Mail oder Passwort.";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDto login(LoginRequestDto request) {

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(MSG_BAD_CREDENTIALS, HttpStatus.UNAUTHORIZED));

        // 1. Hat er seine E-Mail verifiziert?
        if (!user.isEnabled()) {
            throw new AppException("Bitte bestätige zuerst deine E-Mail-Adresse!", HttpStatus.FORBIDDEN);
        }

        // 2. Ist der Account gesperrt (zu viele Fehlversuche)?
        if (!user.isAccountNonLocked()) {
            throw new AppException(
                    "Konto wurde aus Sicherheitsgründen gesperrt. Bitte nutze die 'Passwort vergessen'-Funktion.",
                    HttpStatus.FORBIDDEN);
        }

        // 3. Stimmt das Passwort?
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailedLogin(user);
            if (!user.isAccountNonLocked()) {
                throw new AppException(
                        "Zu viele Fehlversuche! Das Konto wurde aus Sicherheitsgründen gesperrt.",
                        HttpStatus.FORBIDDEN);
            }
            throw new AppException(MSG_BAD_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        // 4. Passwort richtig -> Fehlversuche zurücksetzen
        resetFailedAttempts(user);

        // 5. Token generieren und zurückgeben
        return new AuthResponseDto(jwtService.generateToken(user), user.getRole());
    }

    // --- HILFSMETHODEN FÜR DAS ACCOUNT LOCKING ---

    private void registerFailedLogin(AppUser user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountNonLocked(false);
        }
        appUserRepository.save(user);
    }

    private void resetFailedAttempts(AppUser user) {
        user.setFailedLoginAttempts(0);
        appUserRepository.save(user);
    }
}