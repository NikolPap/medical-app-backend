package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequestDto(
        @NotBlank(message = "Bitte gib deine E-Mail Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail Adresse ein.")
        String email,

        @NotBlank(message = "Bitte gib den Code ein.")
        @Pattern(regexp = "\\d{6}", message = "Der Code muss aus genau 6 Ziffern bestehen.")
        String code,

        @NotBlank(message = "Bitte gib ein neues Passwort ein.")
        @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,72}",
                message = "Das Passwort muss 6-72 Zeichen lang sein und Groß-, Kleinbuchstaben und eine Zahl enthalten.")
        String newPassword
) {}