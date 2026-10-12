package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequestDto(
        @NotBlank(message = "Bitte gib deine E-Mail Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail Adresse ein.")
        String email,

        @NotBlank(message = "Bitte gib den Code ein.")
        @Pattern(regexp = "\\d{6}", message = "Der Code muss aus genau 6 Ziffern bestehen.")
        String code
) {}