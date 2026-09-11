package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.entity.DoctorHoliday;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.service.DoctorHolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class DoctorHolidayController {

    private final DoctorHolidayService doctorHolidayService;
    private static final String RESPONSE_KEY_ERROR = "error";

    @PostMapping
    public ResponseEntity<Object> addHoliday(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // SonarQube Fix: Null Check!
            if (auth == null || auth.getName() == null) {
                throw new AppException("Authentifizierungsdaten fehlen.");
            }

            doctorHolidayService.addHolidayRange(auth.getName(), startDate, endDate);
            return ResponseEntity.ok(new RegisterResponseDto("Urlaub erfolgreich eingetragen."));
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(RESPONSE_KEY_ERROR, e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<List<DoctorHoliday>> getMyHolidays() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // SonarQube Fix: Null Check!
        if (auth == null || auth.getName() == null) {
            throw new AppException("Authentifizierungsdaten fehlen.");
        }

        return ResponseEntity.ok(doctorHolidayService.getMyHolidays(auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteHoliday(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // SonarQube Fix: Null Check!
            if (auth == null || auth.getName() == null) {
                throw new AppException("Authentifizierungsdaten fehlen.");
            }

            doctorHolidayService.deleteHoliday(id, auth.getName());
            return ResponseEntity.ok(new RegisterResponseDto("Urlaub erfolgreich gelöscht."));
        } catch (AppException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(RESPONSE_KEY_ERROR, e.getMessage()));
        }
    }
}