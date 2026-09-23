package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponseDto toResponseDto(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getDoctor().getId(), // 2. Platz: doctorId

                // Arzt Infos
                appointment.getDoctor().getEmail(),
                appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                appointment.getDoctor().getSpecialty(),
                appointment.getDoctor().getAddress(),
                appointment.getDoctor().getPhoneNumber(),

                // Patient Infos
                appointment.getPatient().getEmail(),
                appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                appointment.getPatient().getPhoneNumber(),
                appointment.getPatient().getGender(),
                appointment.getPatient().getInsuranceType(),

                appointment.getAppointmentDate(),
                appointment.getStatus()
        );
    }
}