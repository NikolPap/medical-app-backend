package com.portofolio.medical_app.dto;

import java.time.LocalTime;

public record ScheduleRequestDto(
        String dayOfWeek, // z.B. "MONDAY", "TUESDAY"
        LocalTime startTime, // z.B. "09:00"
        LocalTime endTime    // z.B. "17:00"
) {}