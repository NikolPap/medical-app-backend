package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.DoctorScheduleResponseDto;
import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.dto.ScheduleRequestDto;
import com.portofolio.medical_app.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_DOCTOR')") // sperrt den kompletten Controller für Patienten
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @PostMapping
    public ResponseEntity<RegisterResponseDto> saveSchedule(
            @RequestBody ScheduleRequestDto request,
            Authentication authentication) {
        doctorScheduleService.saveSchedule(request, authentication.getName());
        return ResponseEntity.ok(new RegisterResponseDto("Arbeitszeit erfolgreich gespeichert."));
    }

    @GetMapping("/me")
    public ResponseEntity<List<DoctorScheduleResponseDto>> getMySchedule(Authentication authentication) {
        return ResponseEntity.ok(doctorScheduleService.getMySchedule(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RegisterResponseDto> deleteSchedule(
            @PathVariable Long id,
            Authentication authentication) {
        doctorScheduleService.deleteSchedule(id, authentication.getName());
        return ResponseEntity.ok(new RegisterResponseDto("Arbeitszeit erfolgreich gelöscht."));
    }

    // Termin-Dauer des Arztes ändern (z.B. 15, 30 oder 45 Minuten)
    @PutMapping("/duration")
    public ResponseEntity<RegisterResponseDto> updateSlotDuration(
            @RequestParam int minutes,
            Authentication authentication) {
        doctorScheduleService.updateSlotDuration(minutes, authentication.getName());
        return ResponseEntity.ok(new RegisterResponseDto("Termin-Dauer erfolgreich auf " + minutes + " Minuten geändert."));
    }
}