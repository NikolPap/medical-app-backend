package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.RegisterRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.AppUserMapper;
import com.portofolio.medical_app.mapper.VerificationTokenMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final VerificationTokenMapper verificationTokenMapper;

    //Die Methode zur Registrierung
    public void register(RegisterRequestDto request) {

        //1. Prüfen: Gibt es diese E-mail schon in der Datenbank?
        if (appUserRepository.findByEmail(request.email()).isPresent()) {
            throw new AppException("Diese Email existiert bereits!"); //Bricht den Vorgang mit Fehler ab
        }

        //2. Das Passwort verschlüsseln
        String encodedPassword = passwordEncoder.encode(request.password());
        AppUser newUser = appUserMapper.toEntity(request, encodedPassword);
        AppUser savedUser = appUserRepository.save(newUser);

        //Code generieren und verschlüsseln
        String randomCode = String.format("%06d", secureRandom.nextInt(999999));
        String hashedCode = passwordEncoder.encode(randomCode);

       // Mapping nutzen
        VerificationToken tokenEntity = verificationTokenMapper.toEntity(hashedCode, savedUser);
        verificationTokenRepository.save(tokenEntity);

        //Den UNVERSCHLÜSSELTEN Code an den User schicken
        emailService.sendVerificationEmail(savedUser.getEmail(), randomCode);
    }

}
