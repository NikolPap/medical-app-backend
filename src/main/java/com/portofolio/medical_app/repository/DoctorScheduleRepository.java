package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;


@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    // Holt den Arbeitsplan eines Arztes für einen bestimmten Wochentag

    // Holt den kompletten Wochenplan eines Arztes (Für sein Dashboard)
    List<DoctorSchedule> findByDoctorId(Long doctorId);

    List<DoctorSchedule> findAllByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}