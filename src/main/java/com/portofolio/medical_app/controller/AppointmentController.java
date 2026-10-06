package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private static final String ROLE_DOCTOR = "ROLE_DOCTOR";

    private final AppointmentService appointmentService;

    // Termin buchen (Patient für sich selbst, Arzt für einen Patienten, z.B. beim Verschieben)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR')")
    public ResponseEntity<AppointmentResponseDto> createAppointment(
            @RequestBody AppointmentRequestDto request,
            Authentication authentication) {

        final String patientEmail;

        if (hasRole(authentication, ROLE_DOCTOR)) {
            // Der Arzt MUSS die E-Mail des Patienten mitschicken
            if (request.patientEmail() == null || request.patientEmail().isBlank()) {
                throw new AppException("Bitte wähle einen Patienten aus.");
            }
            patientEmail = request.patientEmail();
        } else {
            // Ein Patient bucht immer für sich selbst (Feld im Request wird ignoriert)
            patientEmail = authentication.getName();
        }

        return ResponseEntity.ok(appointmentService.createAppointment(request, patientEmail));
    }

    // Alle "besetzten" Uhrzeiten eines Arztes an einem Tag
    @GetMapping("/booked-slots")
    public ResponseEntity<List<LocalDateTime>> getBookedSlots(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(appointmentService.getBookedTimeSlots(doctorId, date));
    }

    // Mein Dashboard (meine Termine, egal ob Arzt oder Patient)
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR')")
    public ResponseEntity<List<AppointmentResponseDto>> getMyAppointments(Authentication authentication) {
        String userEmail = authentication.getName();

        List<AppointmentResponseDto> myAppointments = hasRole(authentication, ROLE_DOCTOR)
                ? appointmentService.getAppointmentsForDoctor(userEmail)
                : appointmentService.getAppointmentsForPatient(userEmail);

        return ResponseEntity.ok(myAppointments);
    }

    // Status ändern (Ärzte UND Patienten, die Regeln prüft der Service)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'ROLE_DOCTOR')")
    public ResponseEntity<AppointmentResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        String userRole = authentication.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(
                appointmentService.updateAppointmentStatus(id, status, authentication.getName(), userRole));
    }

    // Öffentlich: freie Uhrzeiten (siehe SecurityConfig)
    @GetMapping("/available-slots")
    public ResponseEntity<List<String>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableTimeSlots(doctorId, date));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}