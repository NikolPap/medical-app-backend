package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.PendingDoctorResponseDto;
import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/doctors/pending")
    public ResponseEntity<List<PendingDoctorResponseDto>> getPendingDoctors() {
        return ResponseEntity.ok(adminService.getPendingDoctors());
    }

    @PutMapping("/doctors/{id}/approve")
    public ResponseEntity<RegisterResponseDto> approve(@PathVariable Long id) {
        adminService.approveDoctor(id);
        return ResponseEntity.ok(new RegisterResponseDto("Arzt wurde freigeschaltet."));
    }

    @PutMapping("/doctors/{id}/reject")
    public ResponseEntity<RegisterResponseDto> reject(@PathVariable Long id) {
        adminService.rejectDoctor(id);
        return ResponseEntity.ok(new RegisterResponseDto("Arzt wurde abgelehnt."));
    }

}