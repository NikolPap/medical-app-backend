package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDto(
        @NotBlank(message = "Bitte gib deine E-Mail Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail Adresse ein.")
        String email
) {}