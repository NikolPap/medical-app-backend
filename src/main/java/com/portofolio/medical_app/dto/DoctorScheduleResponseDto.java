package com.portofolio.medical_app.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DoctorScheduleResponseDto(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
