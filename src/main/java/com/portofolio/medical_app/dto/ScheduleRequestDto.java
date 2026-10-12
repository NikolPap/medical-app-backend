package com.portofolio.medical_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ScheduleRequestDto(
        @NotBlank(message = "Bitte wähle einen Wochentag.") String dayOfWeek,
        @NotNull(message = "Bitte gib eine Startzeit an.") LocalTime startTime,
        @NotNull(message = "Bitte gib eine Endzeit an.") LocalTime endTime
) {}