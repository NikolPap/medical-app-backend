package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.entity.DoctorHoliday;
import com.portofolio.medical_app.service.DoctorHolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_DOCTOR')")
public class DoctorHolidayController {

    private final DoctorHolidayService doctorHolidayService;

    @PostMapping
    public ResponseEntity<RegisterResponseDto> addHoliday(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {
        doctorHolidayService.addHolidayRange(authentication.getName(), startDate, endDate);
        return ResponseEntity.ok(new RegisterResponseDto("Urlaub erfolgreich eingetragen."));
    }

    @GetMapping("/me")
    public ResponseEntity<List<DoctorHoliday>> getMyHolidays(Authentication authentication) {
        return ResponseEntity.ok(doctorHolidayService.getMyHolidays(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RegisterResponseDto> deleteHoliday(
            @PathVariable Long id,
            Authentication authentication) {
        doctorHolidayService.deleteHoliday(id, authentication.getName());
        return ResponseEntity.ok(new RegisterResponseDto("Urlaub erfolgreich gelöscht."));
    }
}