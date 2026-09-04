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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_PENDING = "PENDING";

    private final AppointmentRepository appointmentRepository;
    private final AppUserRepository appUserRepository;
    private final AppointmentMapper appointmentMapper;
    private final EmailService emailService;

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
        appointment.setStatus(STATUS_PENDING);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return appointmentMapper.toResponseDto(savedAppointment);
    }

    //Diese Methode gibt eine Liste von Uhrzeiten zurück, die beim Arzt schon "besetzt" sind
    public java.util.List<LocalDateTime> getBookedTimeSlots(Long doctorId, java.time.LocalDate date) {
        //Wir definieren den Anfang und das Ende des Tages
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23,59,59);

        //Wir holen alle Termine aus der Datenbank
        java.util.List<Appointment> bookedAppointments =appointmentRepository.findAppointmentsByDoctorAndDay(doctorId, startOfDay, endOfDay);

        //Wir extrahieren nur das Datum/Uhrzeit aus den Terminen (Keine Patientendaten wegen DAtenschutz!)
        return bookedAppointments.stream()
                .map(Appointment::getAppointmentDate)
                .toList();
    }

    //Hole alle Termine für den eingeloggten Patienten
    public java.util.List<AppointmentResponseDto> getAppointmentsForPatient(String patientEmail) {

        java.util.List<Appointment> appointments =
       appointmentRepository.findByPatient_EmailOrderByAppointmentDateDesc(patientEmail);

        return appointments.stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    //Hole alle Termine für den eingeloggten Arzt
    public java.util.List<AppointmentResponseDto> getAppointmentsForDoctor(String doctorEmail) {

        java.util.List<Appointment> appointments =
                appointmentRepository.findByDoctor_EmailOrderByAppointmentDateDesc(doctorEmail);

        return appointments.stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    //Methode zum Aktualisieren des Status (Nur für Ärzte)
    public AppointmentResponseDto updateAppointmentStatus(Long appointmentId, String newStatus, String userEmail,String userRole ) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("Termin nicht gefunden"));

        //Status validieren
        if (!STATUS_CONFIRMED.equals(newStatus) && !STATUS_CANCELLED.equals(newStatus)) {
            throw new AppException("Ungültiger Status. Erlaubt sind nur CONFIRMED oder CANCELLED.");
        }

        //Regeln für den Arzt
        if ("ROLE_DOCTOR".equals(userRole)) {
            if (!appointment.getDoctor().getEmail().equals(userEmail)) {
                throw new AppException("Du bist nicht berechtigt, diesen Termin zu ändern.");
            }
        }
        // REGELN FÜR DEN PATIENTEN
        else if ("ROLE_PATIENT".equals(userRole)) {
            if (!appointment.getPatient().getEmail().equals(userEmail)) {
                throw new AppException("Dies ist nicht dein Termin.");
            }
            // Patienten dürfen Termine NUR absagen, niemals bestätigen!
            if (STATUS_CONFIRMED.equals(newStatus)) {
                throw new AppException("Patienten können Termine nicht selbst bestätigen.");
            }
         //48-Stunden-Regel: Absage nur, wenn der Termin mehr als 48 Stunden in der Zukunft liegt!
            ZonedDateTime nowZoned = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime appointmentZoned = appointment.getAppointmentDate().atZone(ZoneId.systemDefault());

            long hoursUntilAppointment = ChronoUnit.HOURS.between(nowZoned, appointmentZoned);

            if (hoursUntilAppointment < 48) {
                throw new AppException("Eine Stornierung ist nur bis 48 Stunden vor dem Termin möglich. Bitte kontaktiere die Praxis telefonisch.");
            }
        }
        //STATUS ÄNDERN UND SPEICHERN
        appointment.setStatus(newStatus);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        if (STATUS_CONFIRMED.equals(newStatus)) {
            emailService.sendStatusEmail(appointment.getPatient().getEmail(), "Dein Termin am " + appointment.getAppointmentDate() + " wurde vom Arzt BESTÄTIGT.");
        } else {
            // SonarQube Fix: Da es nur CONFIRMED oder CANCELLED geben kann, ist hier automatisch CANCELLED!
            emailService.sendStatusEmail(appointment.getPatient().getEmail(), "Der Termin am " + appointment.getAppointmentDate() + " wurde STORNIERT.");
            emailService.sendStatusEmail(appointment.getDoctor().getEmail(), "Ein Termin am " + appointment.getAppointmentDate() + " wurde STORNIERT.");
        }

        return appointmentMapper.toResponseDto(updatedAppointment);
    }
}
