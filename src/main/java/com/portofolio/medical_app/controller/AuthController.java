package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.RegisterRequestDto;
import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.dto.VerificationRequestDto;
import com.portofolio.medical_app.exception.UserAlreadyExistsException;
import com.portofolio.medical_app.service.AuthService;
import com.portofolio.medical_app.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final VerificationService verificationService;

    //POST-Request zum Registrieren (URL: http://localhost:8080/api/auth/register)
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto request) {
        try {
            //Wir rufen unseren Service auf , um die Logik auszuführen
            authService.register(request);

            RegisterResponseDto response = new RegisterResponseDto("Benutzer wurde erfolgreich registriert!");

            //Wenn alles klappt, senden wir ein HTTP 200(OK) an das Frontend
            return ResponseEntity.ok(response);

        }   catch (UserAlreadyExistsException e) {
            //Wenn die E-mail schon existiert, senden wir ein HTTP 400(Bad  Request)
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
}
