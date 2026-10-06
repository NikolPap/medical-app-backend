package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.*;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.service.AuthService;
import com.portofolio.medical_app.service.LoginService;
import com.portofolio.medical_app.service.PasswordResetService;
import com.portofolio.medical_app.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final VerificationService verificationService;
    private final LoginService loginService;
    private final PasswordResetService passwordResetService;
    private final AppUserRepository appUserRepository;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.ok(new RegisterResponseDto("Benutzer wurde erfolgreich registriert!"));
    }

    @PostMapping("/verify")
    public ResponseEntity<RegisterResponseDto> verify(@RequestBody VerificationRequestDto request) {
        verificationService.verifyAccount(request);
        return ResponseEntity.ok(new RegisterResponseDto("Konto wurde erfolgreich verifiziert und entsperrt!"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    // "Ich habe mein Passwort vergessen!" (schickt den Code per E-Mail)
    @PostMapping("/forgot-password")
    public ResponseEntity<RegisterResponseDto> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(new RegisterResponseDto("Falls die E-Mail existiert, wurde ein Code gesendet."));
    }

    // "Hier ist mein Code und das neue Passwort!"
    @PostMapping("/reset-password")
    public ResponseEntity<RegisterResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new RegisterResponseDto("Passwort wurde erfolgreich geändert. Konto ist entsperrt!"));
    }

    // "Ist mein 6-stelliger Code richtig?" (Schritt 1 im Frontend)
    @PostMapping("/verify-reset-code")
    public ResponseEntity<RegisterResponseDto> verifyResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (email == null || code == null) {
            throw new AppException("Email oder Code fehlt!");
        }

        passwordResetService.verifyResetCode(email, code);
        return ResponseEntity.ok(new RegisterResponseDto("Code ist korrekt!"));
    }


    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> getAllDoctors() {
        List<AppUser> doctors = appUserRepository.findByRole("ROLE_DOCTOR");

        List<Map<String, Object>> doctorList = doctors.stream().map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doc.getId());
            map.put("email", doc.getEmail());
            map.put("firstName", doc.getFirstName());
            map.put("lastName", doc.getLastName());
            map.put("specialty", doc.getSpecialty());
            map.put("address", doc.getAddress());
            map.put("slotDuration", doc.getSlotDuration());
            return map;
        }).toList();

        return ResponseEntity.ok(doctorList);
    }
}