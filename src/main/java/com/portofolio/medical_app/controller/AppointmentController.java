package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // POST-Request zum Buchen eines Termins (Jetzt für Patienten UND Ärzte beim Verschieben)
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_DOCTOR')") // <-- WICHTIG: Beide Rollen erlauben
    public ResponseEntity<Object> createAppointment(@RequestBody AppointmentRequestDto request) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                throw new AppException("Authentifizierungsdaten fehlen. Bitte logge dich erneut ein.");
            }

            // Wir holen die Rolle aus dem Token
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String patientEmailForBooking;

            // HIER IST DEIN CODE:
            // Wenn der Arzt bucht, MUSS er uns die E-Mail des Patienten mitschicken
            // Wenn der Patient bucht, ignorieren wir das Feld und nehmen die E-Mail aus seinem Token!
            if ("ROLE_DOCTOR".equals(role)) {
                if (request.patientEmail() == null || request.patientEmail().isEmpty()) {
                    throw new AppException("Bitte wähle einen Patienten aus.");
                }
                patientEmailForBooking = request.patientEmail();
            } else {
                patientEmailForBooking = authentication.getName(); // Patient bucht für sich selbst
            }

            // Wir übergeben die richtige E-Mail an den Service!
            AppointmentResponseDto response = appointmentService.createAppointment(request, patientEmailForBooking);

            return ResponseEntity.ok(response);

        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
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
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_DOCTOR')")
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
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_DOCTOR')")
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
}