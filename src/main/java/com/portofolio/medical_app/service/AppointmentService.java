package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Messages;
import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.AppointmentRequestDto;
import com.portofolio.medical_app.dto.AppointmentResponseDto;
import com.portofolio.medical_app.dto.RescheduleRequestDto;
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
import org.springframework.transaction.annotation.Transactional;

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

    @PreAuthorize("hasAuthority('ROLE_PATIENT') or hasAuthority('ROLE_DOCTOR')")
    @Transactional
    public AppointmentResponseDto createAppointment(
            final AppointmentRequestDto request,
            final String patientEmail
    ) {
        final LocalTime time = LocalTime.parse(request.appointmentTime());

        AppUser patient = appUserRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new AppException("Patient nicht gefunden."));

        AppUser doctor = appUserRepository.findById(request.doctorId())
                .filter(user -> Roles.DOCTOR.equals(user.getRole()))
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));

        Appointment saved = bookSlot(doctor, patient, request.appointmentDate(), time, AppointmentStatus.PENDING);
        return appointmentMapper.toResponseDto(saved);
    }

    public List<LocalDateTime> getBookedTimeSlots(Long doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return appointmentRepository.findAppointmentsByDoctorAndDay(doctorId, startOfDay, endOfDay)
                .stream()
                .map(Appointment::getAppointmentDate)
                .toList();
    }

    public List<AppointmentResponseDto> getAppointmentsForPatient(String patientEmail) {
        return appointmentRepository.findByPatient_EmailOrderByAppointmentDateDesc(patientEmail)
                .stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    public List<AppointmentResponseDto> getAppointmentsForDoctor(String doctorEmail) {
        return appointmentRepository.findByDoctor_EmailOrderByAppointmentDateDesc(doctorEmail)
                .stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

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

    @Transactional
    public AppointmentResponseDto updateAppointmentStatus(
            Long appointmentId, AppointmentStatus newStatus, String userEmail, String userRole) {

        Appointment appointment = findAppointment(appointmentId);
        checkOwnership(appointment, userEmail, userRole);

        if (Roles.PATIENT.equals(userRole) && newStatus == AppointmentStatus.CONFIRMED) {
            throw new AppException("Patienten können Termine nicht selbst bestätigen.", HttpStatus.FORBIDDEN);
        }

        AppointmentStatus current = appointment.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw new AppException("Der Status kann nicht von '" + current.getLabel()
                    + "' auf '" + newStatus.getLabel() + "' geändert werden.");
        }

        checkModifiable(appointment, userRole);

        appointment.setStatus(newStatus);
        Appointment updated = appointmentRepository.save(appointment);

        sendMail(updated, "ist jetzt " + newStatus.getLabel() + ".");
        return appointmentMapper.toResponseDto(updated);
    }

    // Verschieben = alter Termin storniert + neuer Termin, in EINER Transaktion
    @Transactional
    public AppointmentResponseDto rescheduleAppointment(
            Long appointmentId, RescheduleRequestDto request, String userEmail, String userRole) {

        Appointment old = findAppointment(appointmentId);
        checkOwnership(old, userEmail, userRole);

        if (!old.getStatus().canTransitionTo(AppointmentStatus.CANCELLED)) {
            throw new AppException("Dieser Termin kann nicht mehr verschoben werden.");
        }
        checkModifiable(old, userRole);

        final LocalTime time = LocalTime.parse(request.appointmentTime());
        if (request.appointmentDate().atTime(time).equals(old.getAppointmentDate())) {
            throw new AppException("Das ist bereits dein aktueller Termin.");
        }

        // Arzt verschiebt → direkt bestätigt, Patient verschiebt → wartet auf Bestätigung
        AppointmentStatus newStatus = Roles.DOCTOR.equals(userRole)
                ? AppointmentStatus.CONFIRMED
                : AppointmentStatus.PENDING;

        old.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(old);

        // Schlägt das hier fehl, wird auch das Stornieren zurückgerollt
        Appointment created = bookSlot(old.getDoctor(), old.getPatient(),
                request.appointmentDate(), time, newStatus);

        sendMail(created, "wurde verschoben und ist jetzt " + newStatus.getLabel() + ".");
        return appointmentMapper.toResponseDto(created);
    }

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
        List<Appointment> booked = appointmentRepository.findAppointmentsByDoctorAndDay(
                doctorId, date.atStartOfDay(), date.atTime(23, 59, 59));
        List<String> availableSlots = new ArrayList<>();

        for (DoctorSchedule schedule : dailySchedules) {
            LocalTime currentSlotTime = schedule.getStartTime();
            LocalTime workEnd = schedule.getEndTime();

            while (!currentSlotTime.plusMinutes(slotDurationMinutes).isAfter(workEnd)) {
                final LocalDateTime slotStart = date.atTime(currentSlotTime);
                final LocalDateTime slotEnd = slotStart.plusMinutes(slotDurationMinutes);

                // Überschneidung mit irgendeinem bestehenden Termin (egal mit welcher Dauer er gebucht wurde)
                boolean overlapsBooking = booked.stream().anyMatch(a ->
                        slotStart.isBefore(a.getAppointmentDate().plusMinutes(a.getDurationMinutes()))
                                && slotEnd.isAfter(a.getAppointmentDate()));

                if (!overlapsBooking && !slotStart.isBefore(now)) {
                    availableSlots.add(currentSlotTime.toString());
                }
                currentSlotTime = currentSlotTime.plusMinutes(slotDurationMinutes);
            }
        }

        return availableSlots.stream().sorted().toList();
    }

    // ---------- Helfer ----------

    private Appointment bookSlot(AppUser doctor, AppUser patient,
                                 LocalDate date, LocalTime time, AppointmentStatus status) {
        LocalDateTime dateTime = date.atTime(time);

        if (dateTime.isBefore(LocalDateTime.now(clock))) {
            throw new AppException("Termine können nicht in der Vergangenheit gebucht werden.");
        }

        // Schicht, Urlaub, Slot-Raster und bestehende Buchungen
        if (!getAvailableTimeSlots(doctor.getId(), date).contains(time.toString())) {
            throw new AppException("Dieser Zeitpunkt ist nicht verfügbar. Bitte wähle einen anderen Termin.");
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(dateTime);
        // Βεβαιώσου ότι έχεις προσθέσει το durationMinutes στο Appointment Entity σου αν δεν υπήρχε ήδη
        appointment.setDurationMinutes(doctor.getSlotDuration());
        appointment.setStatus(status);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        try {
            return appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new AppException("Dieser Termin ist beim ausgewählten Arzt bereits belegt.", HttpStatus.CONFLICT);
        }
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException("Termin nicht gefunden.", HttpStatus.NOT_FOUND));
    }

    private void checkOwnership(Appointment appointment, String userEmail, String userRole) {
        if (Roles.DOCTOR.equals(userRole)) {
            if (!appointment.getDoctor().getEmail().equals(userEmail)) {
                throw new AppException("Du bist nicht berechtigt, diesen Termin zu ändern.", HttpStatus.FORBIDDEN);
            }
        } else if (Roles.PATIENT.equals(userRole)) {
            if (!appointment.getPatient().getEmail().equals(userEmail)) {
                throw new AppException("Dies ist nicht dein Termin.", HttpStatus.FORBIDDEN);
            }
        } else {
            throw new AppException("Unbekannte oder unzureichende Berechtigung.", HttpStatus.FORBIDDEN);
        }
    }

    // Nicht in der Vergangenheit; Patienten zusätzlich 48-Stunden-Regel
    private void checkModifiable(Appointment appointment, String userRole) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (appointment.getAppointmentDate().isBefore(now)) {
            throw new AppException("Vergangene Termine können nicht mehr geändert werden.");
        }

        if (Roles.PATIENT.equals(userRole)) {
            ZonedDateTime nowZoned = ZonedDateTime.now(clock);
            ZonedDateTime appointmentZoned = appointment.getAppointmentDate().atZone(clock.getZone());

            if (Duration.between(nowZoned, appointmentZoned).toHours() < 48) {
                throw new AppException("Eine Änderung ist nur bis 48 Stunden vor dem Termin möglich. "
                        + "Bitte kontaktiere die Praxis telefonisch.");
            }
        }
    }

    private void sendMail(Appointment appointment, String text) {
        emailService.sendStatusEmail(
                appointment.getPatient().getEmail(),
                "Dein Termin am " + appointment.getAppointmentDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm")) + " Uhr " + text);
    }
}