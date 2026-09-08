package com.portofolio.medical_app.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentRequestDto(
        Long doctorId,
        LocalDate appointmentDate,
        String appointmentTime   ) {


}
