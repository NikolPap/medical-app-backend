package com.portofolio.medical_app.dto;

import java.time.LocalDateTime;

public record AppointmentRequestDto(
        Long doctorId,
        LocalDateTime appointmentDate) {


}
