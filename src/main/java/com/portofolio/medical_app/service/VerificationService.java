package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.VerificationRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import com.portofolio.medical_app.exception.UserAlreadyExistsException;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final AppUserRepository appUserRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    //----Methode zur Verifizierung------
    @Transactional
    public void verifyAccount(VerificationRequestDto request) {
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden."));

        if (user.isEnabled()) {
            throw new RuntimeException("Konto ist bereits verifiziert.");
        }

        VerificationToken verificationToken = verificationTokenRepository.findByUser_EmailAndUsedFalse(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Kein Bestätigungscode gefunden."));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserAlreadyExistsException("Der Bestätigungscode ist abgelaufen. Bitte registriere dich neu.");
        }

        if(!passwordEncoder.matches(request.code(), verificationToken.getToken())) {
            throw new RuntimeException("Falscher Bestätigungscode!");
        }

        user.setEnabled(true);
        appUserRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }
}
