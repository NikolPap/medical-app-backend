package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    //Wandelt das Datenbank-Object in ein JSON-freundliches DTO um
    public AppointmentResponseDto toResponseDto(final Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getDoctor().getEmail(),
                appointment.getPatient().getEmail(),
                appointment.getAppointmentDate(),
                appointment.getStatus()
        );
    }
    
}
