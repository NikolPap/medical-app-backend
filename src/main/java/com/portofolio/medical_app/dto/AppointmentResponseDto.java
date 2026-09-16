package com.portofolio.medical_app.dto;

import java.time.LocalDateTime;

public record AppointmentResponseDto(
        Long id,
        String doctorEmail,
        String doctorName,
        String doctorSpecialty,
        String patientEmail,
        String patientName,
        java.time.LocalDateTime appointmentDate,
        String status
) {}
