package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.dto.ScheduleRequestDto;
import com.portofolio.medical_app.entity.DoctorSchedule;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private static final String ROLE_DOCTOR = "ROLE_DOCTOR";
    private static final String RESPONSE_KEY_ERROR = "error";

    private final DoctorScheduleService doctorScheduleService;
    private final com.portofolio.medical_app.repository.AppUserRepository appUserRepository;

    // Nur Ärzte dürfen ihren Plan speichern!
    @PostMapping
    public ResponseEntity<Object> saveSchedule(@RequestBody ScheduleRequestDto request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // SonarQube Fix: Defensive Programming (Null Check)
            if (authentication == null || authentication.getName() == null || authentication.getAuthorities().isEmpty()) {
                throw new AppException("Authentifizierungsdaten fehlen oder sind unvollständig.");
            }

            String role = authentication.getAuthorities().iterator().next().getAuthority();

            if (!ROLE_DOCTOR.equals(role)) {
                throw new AppException("Nur Ärzte können Arbeitszeiten festlegen!");
            }

            doctorScheduleService.saveSchedule(request, authentication.getName());
            return ResponseEntity.ok(new RegisterResponseDto("Arbeitszeit erfolgreich gespeichert."));

        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(RESPONSE_KEY_ERROR, e.getMessage()));
        }
    }

    // Holt den Wochenplan des Arztes
    @GetMapping("/me")
    public ResponseEntity<List<DoctorSchedule>> getMySchedule() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // SonarQube Fix: Defensive Programming (Null Check)
        if (authentication == null || authentication.getName() == null) {
            throw new AppException("Authentifizierungsdaten fehlen.");
        }

        return ResponseEntity.ok(doctorScheduleService.getMySchedule(authentication.getName()));
    }

    // DELETE: Eine Schicht löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteSchedule(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // SonarQube Fix: Defensive Programming (Null Check)
            if (authentication == null || authentication.getName() == null || authentication.getAuthorities().isEmpty()) {
                throw new AppException("Authentifizierungsdaten fehlen oder sind unvollständig.");
            }

            String role = authentication.getAuthorities().iterator().next().getAuthority();

            if (!ROLE_DOCTOR.equals(role)) {
                throw new AppException("Nur Ärzte können Arbeitszeiten löschen!");
            }

            doctorScheduleService.deleteSchedule(id, authentication.getName());
            return ResponseEntity.ok(new RegisterResponseDto("Arbeitszeit erfolgreich gelöscht."));

        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(RESPONSE_KEY_ERROR, e.getMessage()));
        }
    }

    // PUT: Termin-Dauer des Arztes ändern (z.B. 15, 30 oder 45 Minuten)
    @PutMapping("/duration")
    public ResponseEntity<Object> updateSlotDuration(@RequestParam int minutes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null || authentication.getAuthorities().isEmpty()) {
                throw new AppException("Authentifizierungsdaten fehlen.");
            }

            if (!ROLE_DOCTOR.equals(authentication.getAuthorities().iterator().next().getAuthority())) {
                throw new AppException("Nur Ärzte können die Termin-Dauer ändern!");
            }

            if (minutes < 5 || minutes > 120) {
                throw new AppException("Die Termin-Dauer muss zwischen 5 und 120 Minuten liegen.");
            }

            com.portofolio.medical_app.entity.AppUser doctor = appUserRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new AppException("Arzt nicht gefunden."));

            doctor.setSlotDuration(minutes);
            appUserRepository.save(doctor);

            return ResponseEntity.ok(new RegisterResponseDto("Termin-Dauer erfolgreich auf " + minutes + " Minuten geändert."));

        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(RESPONSE_KEY_ERROR, e.getMessage()));
        }
    }
}