package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Messages;
import com.portofolio.medical_app.dto.DoctorScheduleResponseDto;
import com.portofolio.medical_app.dto.ScheduleRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorSchedule;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.DoctorScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppUserRepository appUserRepository;
    private final DoctorMapper doctorMapper;

    // Speichert oder aktualisiert den Arbeitsplan für einen Tag
    @Transactional
    public void saveSchedule(ScheduleRequestDto request, String doctorEmail) {
        AppUser doctor = findDoctor(doctorEmail);
        DayOfWeek day = parseDay(request.dayOfWeek());
        validateShift(doctor, day, request, null);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        doctorScheduleRepository.save(schedule);
    }

    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleRequestDto request, String doctorEmail) {
        DoctorSchedule schedule = doctorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException("Arbeitszeit nicht gefunden.", HttpStatus.NOT_FOUND));

        if (!schedule.getDoctor().getEmail().equals(doctorEmail)) {
            throw new AppException("Du darfst nur deine eigenen Arbeitszeiten ändern.", HttpStatus.FORBIDDEN);
        }

        DayOfWeek day = parseDay(request.dayOfWeek());
        validateShift(schedule.getDoctor(), day, request, scheduleId);

        schedule.setDayOfWeek(day);
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        doctorScheduleRepository.save(schedule);
    }

    private AppUser findDoctor(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));
    }

    private DayOfWeek parseDay(String value) {
        try {
            return DayOfWeek.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("Ungültiger Wochentag.");
        }
    }

    private void validateShift(AppUser doctor, DayOfWeek day, ScheduleRequestDto request, Long excludeId) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new AppException("Die Endzeit muss nach der Startzeit liegen.");
        }

        boolean overlaps = doctorScheduleRepository.findByDoctorId(doctor.getId()).stream()
                .filter(s -> !s.getId().equals(excludeId))
                .filter(s -> day.equals(s.getDayOfWeek()))
                .anyMatch(s -> request.startTime().isBefore(s.getEndTime())
                        && request.endTime().isAfter(s.getStartTime()));

        if (overlaps) {
            throw new AppException("Diese Zeit überschneidet sich mit einer bestehenden Arbeitszeit.");
        }
    }

    // Holt den kompletten Wochenplan für das Dashboard des Arztes
    public List<DoctorScheduleResponseDto> getMySchedule(String doctorEmail) {
        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));
        return doctorScheduleRepository.findByDoctorId(doctor.getId())
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    // Löscht eine bestimmte Arbeitszeit
    public void deleteSchedule(Long scheduleId, String doctorEmail) {

        DoctorSchedule schedule = doctorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException("Arbeitszeit nicht gefunden."));

        // Security Check: Gehört dieser Plan wirklich diesem Arzt?
        if (!schedule.getDoctor().getEmail().equals(doctorEmail)) {
            throw new AppException("Du darfst nur deine eigenen Arbeitszeiten löschen.");
        }

        doctorScheduleRepository.delete(schedule);
    }

    public void updateSlotDuration(int minutes, String doctorEmail) {
        if (minutes < 5 || minutes > 120) {
            throw new AppException("Die Termin-Dauer muss zwischen 5 und 120 Minuten liegen.");
        }

        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));

        doctor.setSlotDuration(minutes);
        appUserRepository.save(doctor);
    }
}