package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


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

    //Get-Request: Zeigt alle "besetzten" Uhrzeiten eines Arztes an einem bestimmten Tag
    @GetMapping("/booked-slots")
    public ResponseEntity<java.util.List<java.time.LocalDateTime>> getBookedSlots(
            @RequestParam Long doctorId,
            @RequestParam java.time.LocalDate date) {

        java.util.List<java.time.LocalDateTime> bookedSlots = appointmentService.getBookedTimeSlots(doctorId, date);
        return ResponseEntity.ok(bookedSlots);
    }

    //Get_request: Mein Dashboard (Zeigt meine Termine, egal ob Arzt oder Patient)
    @GetMapping("/me")
    public ResponseEntity<java.util.List<AppointmentResponseDto>> getMyAppointments() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new AppException("Authentifizierungsdaten fehlen.");
        }

        String userEmail = authentication.getName();

        // Wir prüfen also nur, ob die Liste LEER ist!
        if (authentication.getAuthorities().isEmpty()) {
            throw new AppException("Dem Benutzer ist keine Rolle zugewiesen.");
        }

        // Wir holen die Rolle und stellen sicher, dass sie nicht null ist!
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role == null) {
            throw new AppException("Die Rolle des Benutzers ist ungültig (null).");
        }

        java.util.List<AppointmentResponseDto> myAppointments;

        if (role.equals("ROLE_DOCTOR")) {
            myAppointments = appointmentService.getAppointmentsForDoctor(userEmail);
        } else if (role.equals("ROLE_PATIENT")) {
            myAppointments = appointmentService.getAppointmentsForPatient(userEmail);
        } else {
            throw new AppException("Unbekannte Rolle: " + role);
        }

        return ResponseEntity.ok(myAppointments);
    }

    //Put-Request: Status eines Termins ändern (Nur für Ärzte!)
    @PutMapping("/{id}/status")
    public ResponseEntity<Object> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                throw new AppException("Authentifizierungsdaten fehlen.");
            }

            String userEmail = authentication.getName();

            // 1. Wir holen die Rolle aus dem Token (Muss existieren!)
            if (authentication.getAuthorities().isEmpty()) {
                throw new AppException("Keine Rolle gefunden.");
            }
            String userRole = authentication.getAuthorities().iterator().next().getAuthority();

            AppointmentResponseDto response = appointmentService.updateAppointmentStatus(id, status, userEmail, userRole);

            return ResponseEntity.ok(response);

        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/available-slots")
    public ResponseEntity<java.util.List<String>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam java.time.LocalDate date) {

        java.util.List<String> availableSlots = appointmentService.getAvailableTimeSlots(doctorId, date);
        return ResponseEntity.ok(availableSlots);
    }

    // POST: Arzt blockiert einen ganzen Tag (z.B. Urlaub / Feiertag)
    @PostMapping("/block-day")
    public ResponseEntity<Object> blockDay(@RequestParam java.time.LocalDate date) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null || authentication.getAuthorities().isEmpty()) {
                throw new AppException("Authentifizierungsdaten fehlen.");
            }

            String role = authentication.getAuthorities().iterator().next().getAuthority();
            if (!"ROLE_DOCTOR".equals(role)) {
                throw new AppException("Nur Ärzte können Tage blockieren.");
            }

            String doctorEmail = authentication.getName();
            appointmentService.blockEntireDay(doctorEmail, date);

            return ResponseEntity.ok(java.util.Map.of("message", "Tag wurde erfolgreich blockiert."));

        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}