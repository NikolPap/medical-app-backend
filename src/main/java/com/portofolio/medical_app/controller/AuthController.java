package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.*;
import com.portofolio.medical_app.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final VerificationService verificationService;
    private final LoginService loginService;
    private final PasswordResetService passwordResetService;
    private final DoctorService doctorService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.ok(new RegisterResponseDto("Benutzer wurde erfolgreich registriert!"));
    }

    @PostMapping("/verify")
    public ResponseEntity<RegisterResponseDto> verify(@Valid @RequestBody VerificationRequestDto request) {
        verificationService.verifyAccount(request);
        return ResponseEntity.ok(new RegisterResponseDto("Konto wurde erfolgreich verifiziert und entsperrt!"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    // "Ich habe mein Passwort vergessen!" (schickt den Code per E-Mail)
    @PostMapping("/forgot-password")
    public ResponseEntity<RegisterResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(new RegisterResponseDto("Falls die E-Mail existiert, wurde ein Code gesendet."));
    }

    // "Hier ist mein Code und das neue Passwort!"
    @PostMapping("/reset-password")
    public ResponseEntity<RegisterResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new RegisterResponseDto("Passwort wurde erfolgreich geändert. Konto ist entsperrt!"));
    }

    // "Ist mein 6-stelliger Code richtig?" (Schritt 1 im Frontend)
    @PostMapping("/verify-reset-code")
    public ResponseEntity<RegisterResponseDto> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequestDto request) {
        passwordResetService.verifyResetCode(request.email(), request.code());
        return ResponseEntity.ok(new RegisterResponseDto("Code ist korrekt!"));
    }


    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponseDto>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }
}