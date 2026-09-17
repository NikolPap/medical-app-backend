package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    //Wandelt das Datenbank-Object in ein JSON-freundliches DTO um
    public AppointmentResponseDto toResponseDto(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),

                appointment.getDoctor().getEmail(),
                appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                appointment.getDoctor().getSpecialty(),
                appointment.getDoctor().getAddress(),       // NEU
                appointment.getDoctor().getPhoneNumber(),   // NEU

                appointment.getPatient().getEmail(),
                appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                appointment.getPatient().getPhoneNumber(),  // NEU
                appointment.getPatient().getGender(),       // NEU
                appointment.getPatient().getInsuranceType(),// NEU

                appointment.getAppointmentDate(),
                appointment.getStatus()
        );
    }
    
}
