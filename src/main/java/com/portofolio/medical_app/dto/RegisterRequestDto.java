package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "Bitte gib deine E-Mail Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail Adresse ein.")
        @Size(max = 255, message = "Die E-Mail Adresse ist zu lang.")
        String email,

        @NotBlank(message = "Bitte gib ein Passwort ein.")
        @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,72}",
                message = "Das Passwort muss 6-72 Zeichen lang sein und Groß-, Kleinbuchstaben und eine Zahl enthalten.")
        String password,

        @NotBlank(message = "Bitte wähle eine Rolle.")
        String role,

        @NotBlank(message = "Bitte gib deinen Vornamen ein.")
        @Size(min = 2, max = 100, message = "Der Vorname muss 2-100 Zeichen lang sein.")
        String firstName,

        @NotBlank(message = "Bitte gib deinen Nachnamen ein.")
        @Size(min = 2, max = 100, message = "Der Nachname muss 2-100 Zeichen lang sein.")
        String lastName,

        @Size(max = 100, message = "Die Fachrichtung ist zu lang.")
        String specialty,

        @Size(max = 255, message = "Die Adresse ist zu lang.")
        String address,

        @NotBlank(message = "Bitte gib deine Telefonnummer ein.")
        @Pattern(regexp = "^\\+?[0-9\\s\\-/]{6,20}$", message = "Bitte gib eine gültige Telefonnummer ein.")
        String phoneNumber,

        @Pattern(regexp = "MALE|FEMALE|DIVERS", message = "Ungültiges Geschlecht.")
        String gender,

        @Pattern(regexp = "GESETZLICH|PRIVATE", message = "Ungültige Versicherungsart.")
        String insuranceType
) {}