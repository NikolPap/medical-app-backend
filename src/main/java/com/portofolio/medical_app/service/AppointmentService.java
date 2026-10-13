package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Messages;
import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.Appointment;
import com.portofolio.medical_app.entity.AppointmentStatus;
import com.portofolio.medical_app.entity.DoctorSchedule;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.AppointmentMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.AppointmentRepository;
import com.portofolio.medical_app.repository.DoctorHolidayRepository;
import com.portofolio.medical_app.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppUserRepository appUserRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorHolidayRepository doctorHolidayRepository;
    private final AppointmentMapper appointmentMapper;
    private final EmailService emailService;
    private final Clock clock;

    // Neuen Termin erstellen
    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_DOCTOR')")
    public AppointmentResponseDto createAppointment(
            final AppointmentRequestDto request,
            final String patientEmail
    ) {
        final LocalTime time = LocalTime.parse(request.appointmentTime());
        final LocalDateTime fullAppointmentDateTime = request.appointmentDate().atTime(time);

        if (fullAppointmentDateTime.isBefore(LocalDateTime.now(clock))) {
            throw new AppException("Termine können nicht in der Vergangenheit gebucht werden.");
        }

        AppUser patient = appUserRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new AppException("Patient nicht gefunden."));

        AppUser doctor = appUserRepository.findById(request.doctorId())
                .filter(user -> Roles.DOCTOR.equals(user.getRole()))
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));

        // Schicht, Urlaub, Slot-Raster und bestehende Buchungen werden hier geprüft
        List<String> freeSlots = getAvailableTimeSlots(doctor.getId(), request.appointmentDate());
        if (!freeSlots.contains(time.toString())) {
            throw new AppException("Dieser Zeitpunkt ist nicht verfügbar. Bitte wähle einen anderen Termin.");
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(fullAppointmentDateTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        try {
            return appointmentMapper.toResponseDto(appointmentRepository.save(appointment));
        } catch (DataIntegrityViolationException e) {
            // Gleichzeitige Requests: der Unique-Index fängt den zweiten ab
            throw new AppException("Dieser Termin ist beim ausgewählten Arzt bereits belegt.", HttpStatus.CONFLICT);
        }
    }

    // Belegte Uhrzeiten eines Arztes an einem Tag
    public List<LocalDateTime> getBookedTimeSlots(Long doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return appointmentRepository.findAppointmentsByDoctorAndDay(doctorId, startOfDay, endOfDay)
                .stream()
                .map(Appointment::getAppointmentDate)
                .toList();
    }

    // Termine des eingeloggten Patienten
    public List<AppointmentResponseDto> getAppointmentsForPatient(String patientEmail) {
        return appointmentRepository.findByPatient_EmailOrderByAppointmentDateDesc(patientEmail)
                .stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    // Termine des eingeloggten Arztes
    public List<AppointmentResponseDto> getAppointmentsForDoctor(String doctorEmail) {
        return appointmentRepository.findByDoctor_EmailOrderByAppointmentDateDesc(doctorEmail)
                .stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    // Darf der Arzt für diesen Patienten buchen? (nur im eigenen Kalender, nur bestehende Patienten)
    public void verifyDoctorMayBookFor(String doctorEmail, Long requestedDoctorId, String patientEmail) {
        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));

        if (!doctor.getId().equals(requestedDoctorId)) {
            throw new AppException("Du kannst Termine nur in deinem eigenen Kalender anlegen.", HttpStatus.FORBIDDEN);
        }

        if (!appointmentRepository.existsByDoctor_EmailAndPatient_Email(doctorEmail, patientEmail)) {
            throw new AppException("Für diesen Patienten kannst du keinen Termin anlegen.", HttpStatus.FORBIDDEN);
        }
    }

    // Status ändern (Ärzte UND Patienten)
    public AppointmentResponseDto updateAppointmentStatus(
            Long appointmentId, AppointmentStatus newStatus, String userEmail, String userRole) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("Termin nicht gefunden.", HttpStatus.NOT_FOUND));

        // Berechtigung
        if (Roles.DOCTOR.equals(userRole)) {
            if (!appointment.getDoctor().getEmail().equals(userEmail)) {
                throw new AppException("Du bist nicht berechtigt, diesen Termin zu ändern.", HttpStatus.FORBIDDEN);
            }
        } else if (Roles.PATIENT.equals(userRole)) {
            if (!appointment.getPatient().getEmail().equals(userEmail)) {
                throw new AppException("Dies ist nicht dein Termin.", HttpStatus.FORBIDDEN);
            }
            if (newStatus == AppointmentStatus.CONFIRMED) {
                throw new AppException("Patienten können Termine nicht selbst bestätigen.", HttpStatus.FORBIDDEN);
            }
        } else {
            throw new AppException("Unbekannte oder unzureichende Berechtigung.", HttpStatus.FORBIDDEN);
        }

        // Statusmaschine
        AppointmentStatus current = appointment.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw new AppException("Der Status kann nicht von '" + current.getLabel()
                    + "' auf '" + newStatus.getLabel() + "' geändert werden.");
        }

        // Vergangene Termine bleiben unverändert
        LocalDateTime now = LocalDateTime.now(clock);
        if (appointment.getAppointmentDate().isBefore(now)) {
            throw new AppException("Vergangene Termine können nicht mehr geändert werden.");
        }

        // 48-Stunden-Regel nur für Patienten
        if (Roles.PATIENT.equals(userRole)) {
            ZonedDateTime nowZoned = ZonedDateTime.now(clock);
            ZonedDateTime appointmentZoned = appointment.getAppointmentDate().atZone(clock.getZone());

            if (Duration.between(nowZoned, appointmentZoned).toHours() < 48) {
                throw new AppException("Eine Stornierung ist nur bis 48 Stunden vor dem Termin möglich. "
                        + "Bitte kontaktiere die Praxis telefonisch.");
            }
        }

        appointment.setStatus(newStatus);
        Appointment updated = appointmentRepository.save(appointment);

        emailService.sendStatusEmail(
                updated.getPatient().getEmail(),
                "Dein Termin am " + updated.getAppointmentDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm"))
                        + " Uhr ist jetzt " + newStatus.getLabel() + ".");

        return appointmentMapper.toResponseDto(updated);
    }

    // Verfügbarkeits-Engine
    public List<String> getAvailableTimeSlots(Long doctorId, LocalDate date) {

        AppUser doctor = appUserRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));

        int slotDurationMinutes = doctor.getSlotDuration();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<DoctorSchedule> dailySchedules =
                doctorScheduleRepository.findAllByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);

        if (dailySchedules.isEmpty()) {
            return new ArrayList<>();
        }

        if (doctorHolidayRepository.existsByDoctorIdAndHolidayDate(doctorId, date)) {
            return new ArrayList<>(); // Urlaub → keine freien Slots
        }

        final LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDateTime> bookedSlots = getBookedTimeSlots(doctorId, date);
        List<String> availableSlots = new ArrayList<>();

        for (DoctorSchedule schedule : dailySchedules) {
            LocalTime currentSlotTime = schedule.getStartTime();
            LocalTime workEnd = schedule.getEndTime();

            while (currentSlotTime.isBefore(workEnd)) {
                if (currentSlotTime.plusMinutes(slotDurationMinutes).isAfter(workEnd)) {
                    break;
                }

                LocalDateTime slotDateTime = date.atTime(currentSlotTime);
                boolean isAlreadyBooked = bookedSlots.contains(slotDateTime);
                boolean isPast = slotDateTime.isBefore(now);

                if (!isAlreadyBooked && !isPast) {
                    availableSlots.add(currentSlotTime.toString());
                }

                currentSlotTime = currentSlotTime.plusMinutes(slotDurationMinutes);
            }
        }

        return availableSlots.stream().sorted().toList();
    }
}