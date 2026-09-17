package com.portofolio.medical_app.dto;

public record RegisterRequestDto(
        String email,
        String password,
        String role,
        String firstName,
        String lastName,
        String specialty,
        String address,
        String phoneNumber,
        String gender,
        String insuranceType
) {

}
