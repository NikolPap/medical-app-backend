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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String MSG_DOCTOR_NOT_FOUND = "Arzt nicht gefunden.";

    private final AppointmentRepository appointmentRepository;
    private final AppUserRepository appUserRepository;
    private final com.portofolio.medical_app.repository.DoctorScheduleRepository doctorScheduleRepository;
    private final com.portofolio.medical_app.repository.DoctorHolidayRepository doctorHolidayRepository;
    private final AppointmentMapper appointmentMapper;
    private final EmailService emailService;

    // Methode zum Erstellen eines neuen Termins
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_DOCTOR')")
    public AppointmentResponseDto createAppointment(
            final AppointmentRequestDto request,
            final String patientEmail
    ) {
        final LocalDateTime fullAppointmentDateTime =
                request.appointmentDate().atTime(LocalTime.parse(request.appointmentTime()));

        if (fullAppointmentDateTime.isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new AppException("Termine können nicht in der Vergangenheit gebucht werden.");
        }

        AppUser patient = appUserRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new AppException("Patient nicht gefunden."));

        AppUser doctor = appUserRepository.findById(request.doctorId())
                .orElseThrow(() -> new AppException(MSG_DOCTOR_NOT_FOUND));

        if (appointmentRepository.isDoctorBooked(doctor.getId(), fullAppointmentDateTime)) {
            throw new AppException("Dieser Termin ist beim ausgewählten Arzt bereits belegt.");
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(fullAppointmentDateTime);
        appointment.setStatus(STATUS_PENDING);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return appointmentMapper.toResponseDto(savedAppointment);
    }

    // Diese Methode gibt eine Liste von Uhrzeiten zurück, die beim Arzt schon "besetzt" sind
    public java.util.List<LocalDateTime> getBookedTimeSlots(Long doctorId, java.time.LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        java.util.List<Appointment> bookedAppointments = appointmentRepository.findAppointmentsByDoctorAndDay(doctorId, startOfDay, endOfDay);

        return bookedAppointments.stream()
                .map(Appointment::getAppointmentDate)
                .toList();
    }

    // Hole alle Termine für den eingeloggten Patienten
    public java.util.List<AppointmentResponseDto> getAppointmentsForPatient(String patientEmail) {
        java.util.List<Appointment> appointments = appointmentRepository.findByPatient_EmailOrderByAppointmentDateDesc(patientEmail);

        return appointments.stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    // Hole alle Termine für den eingeloggten Arzt
    public java.util.List<AppointmentResponseDto> getAppointmentsForDoctor(String doctorEmail) {
        java.util.List<Appointment> appointments = appointmentRepository.findByDoctor_EmailOrderByAppointmentDateDesc(doctorEmail);

        return appointments.stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    // Methode zum Aktualisieren des Status (Für Ärzte UND Patienten)
    public AppointmentResponseDto updateAppointmentStatus(Long appointmentId, String newStatus, String userEmail, String userRole) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("Termin nicht gefunden."));

        if (!STATUS_CONFIRMED.equals(newStatus) && !STATUS_CANCELLED.equals(newStatus)) {
            throw new AppException("Ungültiger Status. Erlaubt sind nur CONFIRMED oder CANCELLED.");
        }

        if ("ROLE_DOCTOR".equals(userRole)) {
            if (!appointment.getDoctor().getEmail().equals(userEmail)) {
                throw new AppException("Du bist nicht berechtigt, diesen Termin zu ändern.");
            }
        }
        else if ("ROLE_PATIENT".equals(userRole)) {
            if (!appointment.getPatient().getEmail().equals(userEmail)) {
                throw new AppException("Dies ist nicht dein Termin.");
            }
            if (STATUS_CONFIRMED.equals(newStatus)) {
                throw new AppException("Patienten können Termine nicht selbst bestätigen.");
            }

            ZonedDateTime nowZoned = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime appointmentZoned = appointment.getAppointmentDate().atZone(ZoneId.systemDefault());
            long hoursUntilAppointment = ChronoUnit.HOURS.between(nowZoned, appointmentZoned);

            if (hoursUntilAppointment < 48) {
                throw new AppException("Eine Stornierung ist nur bis 48 Stunden vor dem Termin möglich. Bitte kontaktiere die Praxis telefonisch.");
            }
        }
        else {
            throw new AppException("Unbekannte oder unzureichende Berechtigung.");
        }

        appointment.setStatus(newStatus);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        if (STATUS_CONFIRMED.equals(newStatus)) {
            emailService.sendStatusEmail(appointment.getPatient().getEmail(), "Dein Termin am " + appointment.getAppointmentDate() + " wurde vom Arzt BESTÄTIGT.");
        } else {
            emailService.sendStatusEmail(appointment.getPatient().getEmail(), "Der Termin am " + appointment.getAppointmentDate() + " wurde STORNIERT.");
            emailService.sendStatusEmail(appointment.getDoctor().getEmail(), "Ein Termin am " + appointment.getAppointmentDate() + " wurde STORNIERT.");
        }

        return appointmentMapper.toResponseDto(updatedAppointment);
    }

    // DIE INTELLIGENTE VERFÜGBARKEITS-MASCHINE
    public java.util.List<String> getAvailableTimeSlots(Long doctorId, java.time.LocalDate date) {

        AppUser doctor = appUserRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(MSG_DOCTOR_NOT_FOUND));

        int slotDurationMinutes = doctor.getSlotDuration();
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();

        java.util.List<com.portofolio.medical_app.entity.DoctorSchedule> dailySchedules =
                doctorScheduleRepository.findAllByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);

        if (dailySchedules.isEmpty()) {
            return new ArrayList<>();
        }

        // SonarQube Fix: Aufruf der neuen Hilfsmethode macht den Code extrem lesbar!
        if (doctorHolidayRepository.existsByDoctorIdAndHolidayDate(doctorId, date)) {
            return new ArrayList<>(); // Arzt hat Urlaub -> Keine freien Slots!
        }

        java.util.List<LocalDateTime> bookedSlots = getBookedTimeSlots(doctorId, date);
        java.util.List<String> availableSlots = new ArrayList<>();

        for (com.portofolio.medical_app.entity.DoctorSchedule schedule : dailySchedules) {
            LocalTime currentSlotTime = schedule.getStartTime();
            LocalTime workEnd = schedule.getEndTime();

            while (currentSlotTime.isBefore(workEnd)) {
                if (currentSlotTime.plusMinutes(slotDurationMinutes).isAfter(workEnd)) {
                    break;
                }

                LocalDateTime slotDateTime = date.atTime(currentSlotTime);
                boolean isAlreadyBooked = bookedSlots.contains(slotDateTime);
                boolean isPast = slotDateTime.isBefore(LocalDateTime.now(ZoneId.systemDefault()));

                if (!isAlreadyBooked && !isPast) {
                    availableSlots.add(currentSlotTime.toString());
                }

                currentSlotTime = currentSlotTime.plusMinutes(slotDurationMinutes);
            }
        }

        return availableSlots.stream().sorted().toList();
    }

}