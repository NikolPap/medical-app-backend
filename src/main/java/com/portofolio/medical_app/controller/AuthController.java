package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.*;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.AuthService;
import com.portofolio.medical_app.service.LoginService;
import com.portofolio.medical_app.service.PasswordResetService;
import com.portofolio.medical_app.service.VerificationService;
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

    private final com.portofolio.medical_app.repository.AppUserRepository appUserRepository;

    //POST-Request zum Registrieren (URL: http://localhost:8080/api/auth/register)
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto request) {
        try {
            //Wir rufen unseren Service auf , um die Logik auszuführen
            authService.register(request);

            RegisterResponseDto response = new RegisterResponseDto("Benutzer wurde erfolgreich registriert!");

            //Wenn alles klappt, senden wir ein HTTP 200(OK) an das Frontend
            return ResponseEntity.ok(response);

        }   catch (AppException e) {
            //Wenn die E-Mail schon existiert, senden wir ein HTTP 400(Bad  Request)
            RegisterResponseDto errorResponse = new RegisterResponseDto(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //Post-Requst zum Verifizieren(URL: http://localhost:8080/api/auth/verify)
    @PostMapping("/verify")
    public ResponseEntity<RegisterResponseDto> verify(@RequestBody VerificationRequestDto request) {
        try {
           verificationService.verifyAccount(request);

           return ResponseEntity.ok(new RegisterResponseDto("Konto wurde erfolgreich veifiziert und entsperrt!"));

        }catch (RuntimeException e) {
            RegisterResponseDto errorResponse = new RegisterResponseDto(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    //POST-Request zum Einloggen (URL: http://localhost:8080/api/auth/lögin)
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequestDto request) {
        try {
            AuthResponseDto responseDto = loginService.login(request);

            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new RegisterResponseDto(e.getMessage()));
        }
    }

    // POST: "Ich habe mein Passwort vergessen!" (Schickt den Code per E-Mail)
    @PostMapping("/forgot-password")
    public ResponseEntity<RegisterResponseDto> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        try {
            passwordResetService.requestPasswordReset(request);
            return ResponseEntity.ok(new RegisterResponseDto("Falls die E-Mail existiert, wurde ein Code gesendet."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new RegisterResponseDto(e.getMessage()));
        }
    }

    // POST: "Hier ist mein Code und das neue Passwort!"
    @PostMapping("/reset-password")
    public ResponseEntity<RegisterResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok(new RegisterResponseDto("Passwort wurde erfolgreich geändert. Konto ist entsperrt!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new RegisterResponseDto(e.getMessage()));
        }
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<java.util.Map<String, Object>>> getAllDoctors() {

        List<AppUser> doctors = appUserRepository.findByRole("ROLE_DOCTOR");

        List<java.util.Map<String, Object>> doctorList = doctors.stream().map(doc -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", doc.getId());
            map.put("email", doc.getEmail());
            map.put("firstName", doc.getFirstName()); // NEU
            map.put("lastName", doc.getLastName());   // NEU
            map.put("specialty", doc.getSpecialty()); // NEU
            map.put("address", doc.getAddress());     // NEU
            map.put("slotDuration", doc.getSlotDuration());
            return map;
        }).toList();

        return ResponseEntity.ok(doctorList);
    }

    // POST: "Ist mein 6-stelliger Code richtig?" (Für Step 1)
    @PostMapping("/verify-reset-code")
    public ResponseEntity<RegisterResponseDto> verifyResetCode(@RequestBody VerificationRequestDto request) {
        try {
            // Wir nutzen hier das DTO, das wir schon für die normale Verifizierung haben! (Hat email und code)
            passwordResetService.verifyResetCode(request.email(), request.code());
            return ResponseEntity.ok(new RegisterResponseDto("Code ist korrekt!"));
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(new RegisterResponseDto(e.getMessage()));
        }
    }

}
