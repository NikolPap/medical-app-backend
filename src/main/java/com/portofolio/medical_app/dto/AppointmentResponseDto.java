package com.portofolio.medical_app.dto;

import java.time.LocalDateTime;

public record AppointmentResponseDto(
        Long id,
        String doctorEmail,
        String patientEmail,
        LocalDateTime appointmentDate,
        String status
) {
}
