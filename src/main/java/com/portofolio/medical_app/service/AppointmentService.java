package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.Appointment;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.AppointmentMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppUserRepository appUserRepository;
    private final AppointmentMapper appointmentMapper;

    // Methode zum Erstellen eines neuen Termins
    public AppointmentResponseDto createAppointment(AppointmentRequestDto request, String patientEmail) {

        // Termine dürfen nicht in der Vergangenheit liegen!
        if (request.appointmentDate().isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new AppException("Termine können nicht in der Vergangenheit gebucht werden.");
        }

        //Lade den Patienten (Wir wissen durch das JWT genau , wer er ist!)
        AppUser patient = appUserRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new AppException("Patient nicht gefunden"));

        AppUser doctor = appUserRepository.findById(request.doctorId())
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));

        // Sicherheits-check: Hat der Arzt um diese Uhrzeit schon einen Termin?
        if (appointmentRepository.isDoctorBooked(doctor.getId(), request.appointmentDate())) {
            throw new AppException("Dieser Termin ist beim ausgewählten Arzt bereits belegt.");
        }

        //Alles in Ordnung! dem Terminbauen und speichern
        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStatus("PENDING");
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return appointmentMapper.toResponseDto(savedAppointment);
    }

}
