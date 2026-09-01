package com.portofolio.medical_app.dto;

public record ResetPasswordRequestDto(
        String email,
        String code,
        String newPassword
) {
}
