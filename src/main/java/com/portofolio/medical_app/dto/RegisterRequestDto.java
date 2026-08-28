package com.portofolio.medical_app.dto;

public record RegisterRequestDto(
        String email,
        String password,
        String role
) {

}
