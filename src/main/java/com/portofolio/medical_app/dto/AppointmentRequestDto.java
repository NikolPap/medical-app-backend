package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;


public record AppointmentRequestDto(
        @NotNull(message = "Bitte wähle einen Arzt.") Long doctorId,
        @NotNull(message = "Bitte wähle ein Datum.") LocalDate appointmentDate,

        @NotBlank(message = "Bitte wähle eine Uhrzeit.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Ungültige Uhrzeit.")
        String appointmentTime,

        @Email(message = "Ungültige E-Mail Adresse des Patienten.")
        String patientEmail
) {}