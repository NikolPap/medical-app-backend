package com.portofolio.medical_app.repository;


import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.Appointment;
import com.portofolio.medical_app.entity.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false"
})
class AppointmentRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final LocalDateTime SLOT = LocalDate.of(2026, 10, 26).atTime(9, 0);

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AppUserRepository appUserRepository;

    private AppUser doctor;
    private AppUser otherDoctor;
    private AppUser patient;
    private AppUser otherPatient;

    @BeforeEach
    void setUp() {

        doctor = appUserRepository.save(user("doc@example.com", Roles.DOCTOR));
        otherDoctor = appUserRepository.save(user("doc2@example.com", Roles.DOCTOR));
        patient = appUserRepository.save(user("anna@example.com", Roles.PATIENT));
        otherPatient = appUserRepository.save(user("max@example.com", Roles.PATIENT));
    }

    // ---------- Double-Booking-Schutz ----------

    @Test
    void secondActiveAppointmentInTheSameSlotIsRejectedByTheDatabase() {
        appointmentRepository.saveAndFlush(appointment(doctor, patient, SLOT, AppointmentStatus.PENDING));
        Appointment duplicate = appointment(doctor, otherPatient, SLOT, AppointmentStatus.PENDING);

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cancelledAppointmentDoesNotBlockTheSlot() {
        appointmentRepository.saveAndFlush(appointment(doctor, patient, SLOT, AppointmentStatus.CANCELLED));

        Appointment rebooked = appointmentRepository.saveAndFlush(
                appointment(doctor, otherPatient, SLOT, AppointmentStatus.PENDING));

        assertThat(rebooked.getId()).isNotNull();
    }

    @Test
    void sameSlotWithAnotherDoctorIsAllowed() {
        appointmentRepository.saveAndFlush(appointment(doctor, patient, SLOT, AppointmentStatus.CONFIRMED));

        Appointment other = appointmentRepository.saveAndFlush(
                appointment(otherDoctor, patient, SLOT, AppointmentStatus.CONFIRMED));

        assertThat(other.getId()).isNotNull();
    }

    // ---------- Queries ----------

    @Test
    void dayQueryReturnsOnlyActiveAppointmentsOfThatDoctorAndDay() {
        appointmentRepository.save(appointment(doctor, patient, SLOT, AppointmentStatus.PENDING));
        appointmentRepository.save(appointment(doctor, patient, SLOT.plusHours(1), AppointmentStatus.CANCELLED));
        appointmentRepository.save(appointment(doctor, patient, SLOT.plusDays(1), AppointmentStatus.PENDING));
        appointmentRepository.save(appointment(otherDoctor, patient, SLOT, AppointmentStatus.PENDING));
        appointmentRepository.flush();

        List<Appointment> result = appointmentRepository.findAppointmentsByDoctorAndDay(
                doctor.getId(), SLOT.toLocalDate().atStartOfDay(), SLOT.toLocalDate().atTime(23, 59, 59));

        assertThat(result).extracting(Appointment::getAppointmentDate).containsExactly(SLOT);
    }

    @Test
    void patientAppointmentsAreSortedNewestFirst() {
        appointmentRepository.save(appointment(doctor, patient, SLOT, AppointmentStatus.PENDING));
        appointmentRepository.save(appointment(doctor, patient, SLOT.plusDays(2), AppointmentStatus.PENDING));
        appointmentRepository.save(appointment(doctor, otherPatient, SLOT.plusDays(1), AppointmentStatus.PENDING));
        appointmentRepository.flush();

        List<Appointment> result = appointmentRepository
                .findByPatient_EmailOrderByAppointmentDateDesc("anna@example.com");

        assertThat(result).extracting(Appointment::getAppointmentDate)
                .containsExactly(SLOT.plusDays(2), SLOT);
    }

    @Test
    void existsQueryKnowsWhichPatientsBelongToWhichDoctor() {
        appointmentRepository.saveAndFlush(appointment(doctor, patient, SLOT, AppointmentStatus.PENDING));

        assertThat(appointmentRepository
                .existsByDoctor_EmailAndPatient_Email("doc@example.com", "anna@example.com")).isTrue();
        assertThat(appointmentRepository
                .existsByDoctor_EmailAndPatient_Email("doc@example.com", "max@example.com")).isFalse();
        assertThat(appointmentRepository
                .existsByDoctor_EmailAndPatient_Email("doc2@example.com", "anna@example.com")).isFalse();
    }

    @Test
    void countActiveInRangeIgnoresCancelledAndOutOfRange() {
        appointmentRepository.save(appointment(doctor, patient, SLOT, AppointmentStatus.PENDING));
        appointmentRepository.save(appointment(doctor, patient, SLOT.plusHours(1), AppointmentStatus.CONFIRMED));
        appointmentRepository.save(appointment(doctor, patient, SLOT.plusHours(2), AppointmentStatus.CANCELLED));
        appointmentRepository.save(appointment(doctor, patient, SLOT.plusDays(5), AppointmentStatus.PENDING));
        appointmentRepository.flush();

        long count = appointmentRepository.countActiveInRange(
                doctor.getId(), SLOT.toLocalDate().atStartOfDay(), SLOT.toLocalDate().plusDays(1).atStartOfDay());

        assertThat(count).isEqualTo(2);
    }

    // ---------- Helfer ----------

    private AppUser user(String email, String role) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPassword("hash");
        u.setFirstName("Vor");
        u.setLastName("Nach");
        u.setRole(role);
        u.setEnabled(true);
        return u;
    }

    private Appointment appointment(AppUser doc, AppUser pat, LocalDateTime start, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setDoctor(doc);
        a.setPatient(pat);
        a.setAppointmentDate(start);
        a.setDurationMinutes(30);
        a.setStatus(status);
        return a;
    }
}