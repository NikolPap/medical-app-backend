package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.AuthResponseDto;
import com.portofolio.medical_app.dto.LoginRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDto login(LoginRequestDto request) {

        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException("Falsche E-Mail oder Passwort."));

        // 1. Hat er seine E-Mail verifiziert?
        if (!user.isEnabled()) {
            throw new AppException("Bitte bestätige zuerst deine E-Mail-Adresse!");
        }

        // 2. Ist der Account gesperrt (weil er 3x falsch lag)?
        if (!user.isAccountNonLocked()) {
            throw new AppException("Konto wurde aus Sicherheitsgründen gesperrt. Bitte nutze die 'Passwort vergessen'-Funktion.");
        }

        // 3. STIMMT DAS PASSWORT?
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {

            // PASSWORT FALSCH -> Fehlversuche erhöhen!
            registerFailedLogin(user);
           if(!user.isAccountNonLocked()) {
               throw new AppException("Zu viele Fehlversuche! Das Konto wurde aus Sicherheitsgründen gesperrt.");
           }
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
            user.setAccountNonLocked(false);
        }
        appUserRepository.save(user); // Änderungen in der DB speichern
    }

    private void resetFailedAttempts(AppUser user) {
        user.setFailedLoginAttempts(0);
        appUserRepository.save(user);
    }

}
