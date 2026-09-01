package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.AuthResponseDto;
import com.portofolio.medical_app.dto.LoginRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDto login(LoginRequestDto request) {
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Falscher E-mail oder Password."));

        if(!user.isEnabled()) {
            throw new RuntimeException("Bitte bestätige zuerst deine Email-Adresse!");
        }

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Falshe E-Mail oder Password.");
        }

        String jwtToken = jwtService.generateToken(user);

        return new AuthResponseDto(jwtToken, user.getRole());
    }

}
