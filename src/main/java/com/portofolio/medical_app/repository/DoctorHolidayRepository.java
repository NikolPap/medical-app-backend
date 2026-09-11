package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.DoctorHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorHolidayRepository extends JpaRepository<DoctorHoliday, Long> {

    // Prüft, ob der Arzt an diesem Tag Urlaub hat
    boolean existsByDoctorIdAndHolidayDate(Long doctorId, LocalDate date);

    // Holt alle Urlaube eines Arztes (sortiert nach Datum)
    List<DoctorHoliday> findByDoctorIdOrderByHolidayDateAsc(Long doctorId);
}