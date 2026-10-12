package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "Bitte gib deine E-Mail Adresse ein.") String email,
        @NotBlank(message = "Bitte gib dein Passwort ein.") String password
) {}