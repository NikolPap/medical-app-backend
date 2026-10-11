package com.portofolio.medical_app.dto;

public record DoctorResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String specialty,
        String address,
        int slotDuration
) {
}
