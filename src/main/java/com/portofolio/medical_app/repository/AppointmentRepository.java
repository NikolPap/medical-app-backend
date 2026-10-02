package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :apptDate AND a.status != 'CANCELLED'")
    boolean isDoctorBooked(@Param("doctorId") Long doctorId, @Param("apptDate") LocalDateTime apptDate);

    // Wir suchen alle Termine eines bestimmten Arztes an einem bestimmten Tag (von 00:00 bis 23:59 Uhr)
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate >= :startOfDay AND a.appointmentDate <= :endOfDay AND a.status != 'CANCELLED'")
    java.util.List<Appointment> findAppointmentsByDoctorAndDay(
            @Param("doctorId") Long doctorId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // Hole alle Termine eines bestimmten Patienten (Sortiert nach Datum absteigend → Neueste zuerst)
    java.util.List<Appointment> findByPatient_EmailOrderByAppointmentDateDesc(String patientEmail);

    // Hole alle Termine eines bestimmten Arztes (Sortiert nach Datum absteigend)
    java.util.List<Appointment> findByDoctor_EmailOrderByAppointmentDateDesc(String doctorEmail);
}
