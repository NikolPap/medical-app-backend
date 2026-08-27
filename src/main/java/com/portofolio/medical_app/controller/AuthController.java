package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.RegisterRequest;
import com.portofolio.medical_app.service.AuthService;
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

    //POST-Request zum Registrieren (URL: http://localhost:8080/api/auth/register)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            //Wir rufen unseren Service auf , um die Logik auszuführen
            authService.register(request);

            //Wenn alles klappt, senden wir ein HTTP 200(OK) an das Frontend
            return ResponseEntity.ok("Benutzer wurde erfolgreich registriert!");
        } catch (RuntimeException e) {
            //Wenn die E-mail schon existiert, senden wir ein HTTP 400(Bad  Request)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
