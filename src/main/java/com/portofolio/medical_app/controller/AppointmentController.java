package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // Post-Request zum Buchen eines Termins (Nur erreichbar mit gültigem JWT-Token!)
    @PostMapping
    public ResponseEntity<Object> createAppointment(@RequestBody AppointmentRequestDto request) {

        try {
            // Wir hollen uns die Identität des eingeloggten Nutzers!
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                throw new AppException("Authentifizierungsdaten fehlen. Bitte logge dich erneut ein.");
            }

            String patientEmail = authentication.getName();

            AppointmentResponseDto response = appointmentService.createAppointment(request, patientEmail);

            //Wenn alles erfolgreich war, senden wir die Termin-Bestätigung als JSON (HTTP 200) zurück
            return ResponseEntity.ok(response);

        } catch (AppException e) {

            //senden wir die Fehlermeldung als Bad Requst (HTTP 400) an das Frontend
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
