package com.portofolio.medical_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DoctorSchedule extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private AppUser doctor;

    // Der Wochentag (MONDAY, TUESDAY, WEDNESDAY...)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    // Wann beginnt er zu arbeiten? (z.B. 09:00)
    @Column(nullable = false)
    private LocalTime startTime;

    // Wann geht er nach Hause? (z.B. 14:00)
    @Column(nullable = false)
    private LocalTime endTime;
}