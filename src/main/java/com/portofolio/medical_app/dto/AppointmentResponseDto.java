package com.portofolio.medical_app.dto;

public record AppointmentResponseDto(
        Long id,
        Long doctorId,          // 2. Platz: doctorId

        // Arzt Infos
        String doctorEmail,
        String doctorName,
        String doctorSpecialty,
        String doctorAddress,
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
