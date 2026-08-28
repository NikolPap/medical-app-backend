package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.RegisterRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import com.portofolio.medical_app.exception.UserAlreadyExistsException;
import com.portofolio.medical_app.mapper.AppUserMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    //Die Methode zur Registrierung
    public void register(RegisterRequestDto request) {
        //1. Prüfen: Gibt es diese E-mail schon in der Datenbank?
        if(appUserRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Diese Email existiert bereits!"); //Bricht den Vorgang mit Fehler ab
        }

        //2. Das Passwort verschlüsseln
        String encodedPassword = passwordEncoder.encode(request.password());
        AppUser newUser = appUserMapper.toEntity(request, encodedPassword);
        AppUser savedUser = appUserRepository.save(newUser);

        String randomCode = String.format("%06d", secureRandom.nextInt(999999));

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(randomCode);
        verificationToken.setUser(savedUser);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        verificationTokenRepository.save(verificationToken);
    }
}
