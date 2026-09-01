package com.portofolio.medical_app.dto;

public record AuthResponseDto(
        String token,
        String role
) {
}
