package com.portofolio.medical_app.dto;

public record PendingDoctorResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String specialty,
        String address,
        String phoneNumber
)  {
}
