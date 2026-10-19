package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.AppointmentRequestDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    // Montag, 19.10.2026, 10:00 Uhr in Berlin (MESZ, die Umstellung ist erst am 25.10.)
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZONE);

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 26);

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private DoctorScheduleRepository scheduleRepository;
    @Mock private DoctorHolidayRepository holidayRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private EmailService emailService;

    private AppointmentService service;
    private AppUser doctor;
    private AppUser patient;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, appUserRepository, scheduleRepository,
                holidayRepository, appointmentMapper, emailService, CLOCK);

        doctor = user(1L, "doc@example.com", Roles.DOCTOR);
        doctor.setSlotDuration(30);
        patient = user(2L, "anna@example.com", Roles.PATIENT);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appUserRepository.findByEmail("doc@example.com")).thenReturn(Optional.of(doctor));
        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(patient));
        when(holidayRepository.existsByDoctorIdAndHolidayDate(anyLong(), any())).thenReturn(false);
        when(appointmentRepository.findAppointmentsByDoctorAndDay(anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.findAllByDoctorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(shift("09:00", "11:00")));
    }

    // ---------- Verfügbarkeits-Engine ----------

    @Test
    void generatesSlotsFromTheShift() {
        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY))
                .containsExactly("09:00", "09:30", "10:00", "10:30");
    }

    @Test
    void noSlotsOnDaysWithoutShift() {
        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY.plusDays(1))).isEmpty();
    }

    @Test
    void noSlotsOnHolidays() {
        when(holidayRepository.existsByDoctorIdAndHolidayDate(1L, NEXT_MONDAY)).thenReturn(true);

        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY)).isEmpty();
    }

    @Test
    void bookedSlotsAreExcluded() {
        when(appointmentRepository.findAppointmentsByDoctorAndDay(anyLong(), any(), any()))
                .thenReturn(List.of(booked(NEXT_MONDAY.atTime(9, 0), 30)));

        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY))
                .containsExactly("09:30", "10:00", "10:30");
    }

    @Test
    void slotOverlappingAnOldLongerBookingIsExcluded() {
        // Termin wurde früher mit 60 Min gebucht, der Arzt hat inzwischen auf 30 Min umgestellt
        when(appointmentRepository.findAppointmentsByDoctorAndDay(anyLong(), any(), any()))
                .thenReturn(List.of(booked(NEXT_MONDAY.atTime(9, 0), 60)));

        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY))
                .containsExactly("10:00", "10:30");
    }

    @Test
    void slotsInThePastAreExcluded() {
        // "Jetzt" ist 10:00 Uhr: 09:00 und 09:30 sind vorbei
        assertThat(service.getAvailableTimeSlots(1L, TODAY))
                .containsExactly("10:00", "10:30");
    }

    @Test
    void lastSlotMustEndWithinTheShift() {
        doctor.setSlotDuration(45);

        // 09:00-09:45 und 09:45-10:30 passen, 10:30-11:15 nicht mehr
        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY))
                .containsExactly("09:00", "09:45");
    }

    @Test
    void twoShiftsOnTheSameDayAreCombinedAndSorted() {
        when(scheduleRepository.findAllByDoctorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(shift("14:00", "15:00"), shift("09:00", "10:00")));

        assertThat(service.getAvailableTimeSlots(1L, NEXT_MONDAY))
                .containsExactly("09:00", "09:30", "14:00", "14:30");
    }

    // ---------- Buchen ----------

    @Test
    void bookingAValidSlotCreatesAPendingAppointment() {
        service.createAppointment(request("09:30"), "anna@example.com");

        verify(appointmentRepository).save(argThat(a ->
                a.getStatus() == AppointmentStatus.PENDING
                        && a.getAppointmentDate().equals(NEXT_MONDAY.atTime(9, 30))
                        && a.getDurationMinutes() == 30
                        && a.getPatient() == patient
                        && a.getDoctor() == doctor));
    }

    @Test
    void bookingOutsideTheShiftIsRejected() {
        AppointmentRequestDto request = request("03:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.createAppointment(request, "anna@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nicht verfügbar");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookingAnAlreadyBookedSlotIsRejected() {
        when(appointmentRepository.findAppointmentsByDoctorAndDay(anyLong(), any(), any()))
                .thenReturn(List.of(booked(NEXT_MONDAY.atTime(9, 0), 30)));

        AppointmentRequestDto request = request("09:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.createAppointment(request, "anna@example.com"))
                .isInstanceOf(AppException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookingOnAHolidayIsRejected() {
        when(holidayRepository.existsByDoctorIdAndHolidayDate(1L, NEXT_MONDAY)).thenReturn(true);

        AppointmentRequestDto request = request("09:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.createAppointment(request, "anna@example.com"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void bookingInThePastIsRejected() {
        AppointmentRequestDto past = new AppointmentRequestDto(1L, TODAY, "09:00", null);

        assertThatThrownBy(() -> service.createAppointment(past, "anna@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Vergangenheit");
    }

    @Test
    void bookingWithTheIdOfANonDoctorIsRejected() {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(patient));
        AppointmentRequestDto request = new AppointmentRequestDto(2L, NEXT_MONDAY, "09:00", null);

        assertThatThrownBy(() -> service.createAppointment(request, "anna@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Arzt nicht gefunden");
    }

    @Test
    void concurrentDoubleBookingIsTranslatedToConflict() {
        when(appointmentRepository.save(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("uq_appointments_doctor_slot"));

        AppointmentRequestDto request = request("09:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.createAppointment(request, "anna@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ---------- Doctor darf nur für eigene Patienten buchen ----------

    @Test
    void doctorMayBookForHisOwnPatient() {
        when(appointmentRepository.existsByDoctor_EmailAndPatient_Email("doc@example.com", "anna@example.com"))
                .thenReturn(true);

        service.verifyDoctorMayBookFor("doc@example.com", 1L, "anna@example.com");
    }

    @Test
    void doctorCannotBookInAnotherDoctorsCalendar() {
        assertThatThrownBy(() -> service.verifyDoctorMayBookFor("doc@example.com", 99L, "anna@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void doctorCannotBookForAPatientHeHasNeverTreated() {
        when(appointmentRepository.existsByDoctor_EmailAndPatient_Email(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyDoctorMayBookFor("doc@example.com", 1L, "stranger@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---------- Status ändern ----------

    @Test
    void doctorCanConfirmHisOwnPendingAppointment() {
        Appointment appointment = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        service.updateAppointmentStatus(10L, AppointmentStatus.CONFIRMED, "doc@example.com", Roles.DOCTOR);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(emailService).sendStatusEmail(any(), any());
    }

    @Test
    void doctorCannotChangeAnotherDoctorsAppointment() {
        Appointment appointment = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                10L, AppointmentStatus.CONFIRMED, "other-doc@example.com", Roles.DOCTOR))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nicht berechtigt");
    }

    @Test
    void patientCannotConfirm() {
        Appointment appointment = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                10L, AppointmentStatus.CONFIRMED, "anna@example.com", Roles.PATIENT))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nicht selbst bestätigen");
    }

    @Test
    void patientCannotCancelSomeoneElsesAppointment() {
        Appointment appointment = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                10L, AppointmentStatus.CANCELLED, "intruder@example.com", Roles.PATIENT))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nicht dein Termin");
    }

    @Test
    void patientCanCancelMoreThan48HoursBefore() {
        Appointment appointment = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        service.updateAppointmentStatus(10L, AppointmentStatus.CANCELLED, "anna@example.com", Roles.PATIENT);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void patientCannotCancelWithin48Hours() {
        Appointment appointment = appointment(10L, TODAY.plusDays(1).atTime(9, 0), AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                10L, AppointmentStatus.CANCELLED, "anna@example.com", Roles.PATIENT))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("48 Stunden");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void doctorMayCancelWithin48Hours() {
        Appointment appointment = appointment(10L, TODAY.plusDays(1).atTime(9, 0), AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        service.updateAppointmentStatus(10L, AppointmentStatus.CANCELLED, "doc@example.com", Roles.DOCTOR);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancelledAppointmentCannotBeRevived() {
        Appointment appointment = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                10L, AppointmentStatus.CONFIRMED, "doc@example.com", Roles.DOCTOR))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("kann nicht von");
    }

    @Test
    void pastAppointmentsCannotBeChanged() {
        Appointment appointment = appointment(10L, TODAY.minusDays(1).atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                10L, AppointmentStatus.CONFIRMED, "doc@example.com", Roles.DOCTOR))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Vergangene");
    }

    @Test
    void unknownAppointmentResultsInNotFound() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAppointmentStatus(
                99L, AppointmentStatus.CANCELLED, "doc@example.com", Roles.DOCTOR))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---------- Verschieben ----------

    @Test
    void rescheduleByDoctorCancelsOldAndCreatesConfirmedAppointment() {
        Appointment old = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(old));
        when(scheduleRepository.findAllByDoctorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(shift("09:00", "12:00")));

        service.rescheduleAppointment(10L, new RescheduleRequestDto(NEXT_MONDAY, "11:00"),
                "doc@example.com", Roles.DOCTOR);

        assertThat(old.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(argThat(a ->
                a.getStatus() == AppointmentStatus.CONFIRMED
                        && a.getAppointmentDate().equals(NEXT_MONDAY.atTime(11, 0))));
    }

    @Test
    void rescheduleByPatientCreatesPendingAppointment() {
        Appointment old = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(old));

        service.rescheduleAppointment(10L, new RescheduleRequestDto(NEXT_MONDAY, "10:00"),
                "anna@example.com", Roles.PATIENT);

        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.PENDING));
    }

    @Test
    void rescheduleToTheSameTimeIsRejected() {
        Appointment old = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(old));

        RescheduleRequestDto request = new RescheduleRequestDto(NEXT_MONDAY, "09:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.rescheduleAppointment(
                10L, request, "doc@example.com", Roles.DOCTOR))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("aktueller Termin");
    }

    @Test
    void rescheduleToAnInvalidSlotFails() {
        Appointment old = appointment(10L, NEXT_MONDAY.atTime(9, 0), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(old));

        RescheduleRequestDto request = new RescheduleRequestDto(NEXT_MONDAY, "15:00"); // Αλλαγή: Βγήκε έξω

        // 15:00 liegt außerhalb der Schicht. In der echten Transaktion wird das Stornieren zurückgerollt.
        assertThatThrownBy(() -> service.rescheduleAppointment(
                10L, request, "doc@example.com", Roles.DOCTOR))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("nicht verfügbar");
    }

    @Test
    void patientCannotRescheduleWithin48Hours() {
        Appointment old = appointment(10L, TODAY.plusDays(1).atTime(9, 0), AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(old));

        RescheduleRequestDto request = new RescheduleRequestDto(NEXT_MONDAY, "10:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.rescheduleAppointment(
                10L, request, "anna@example.com", Roles.PATIENT))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("48 Stunden");
    }

    // ---------- Helfer ----------

    private AppUser user(Long id, String email, String role) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    private DoctorSchedule shift(String from, String to) {
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctor(doctor);
        s.setDayOfWeek(DayOfWeek.MONDAY);
        s.setStartTime(LocalTime.parse(from));
        s.setEndTime(LocalTime.parse(to));
        return s;
    }

    private Appointment booked(LocalDateTime start, int minutes) {
        Appointment a = new Appointment();
        a.setAppointmentDate(start);
        a.setDurationMinutes(minutes);
        a.setStatus(AppointmentStatus.CONFIRMED);
        a.setDoctor(doctor);
        a.setPatient(patient);
        return a;
    }

    private Appointment appointment(Long id, LocalDateTime start, AppointmentStatus status) {
        Appointment a = booked(start, 30);
        a.setId(id);
        a.setStatus(status);
        return a;
    }

    private AppointmentRequestDto request(String time) {
        return new AppointmentRequestDto(1L, NEXT_MONDAY, time, null);
    }
}