package com.portofolio.medical_app.dto;

public record VerificationRequestDto(
        String email,
        String code

) {
}
