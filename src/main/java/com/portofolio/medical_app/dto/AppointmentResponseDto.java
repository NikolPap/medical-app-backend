package com.portofolio.medical_app.dto;

import java.time.LocalDateTime;

public record AppointmentResponseDto(
        Long id,
        String doctorEmail,
        String doctorName,
        String doctorSpecialty, String doctorAddress,
        String doctorPhone,

        // Patient Infos
        String patientEmail,
        String patientName,
        String patientPhone,
        String patientGender,
        String patientInsurance,
        java.time.LocalDateTime appointmentDate,
        String status
) {}
