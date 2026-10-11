package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.DoctorScheduleResponseDto;
import com.portofolio.medical_app.dto.ScheduleRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorSchedule;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppUserRepository appUserRepository;
    private final DoctorMapper doctorMapper;

    // Speichert oder aktualisiert den Arbeitsplan für einen Tag
    public void saveSchedule(ScheduleRequestDto request, String doctorEmail) {

        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));

        DayOfWeek day = DayOfWeek.valueOf(request.dayOfWeek().toUpperCase());

        // Logik-Check: Feierabend darf nicht vor Arbeitsbeginn sein!
        if (request.endTime().isBefore(request.startTime())) {
            throw new AppException("Die Endzeit darf nicht vor der Startzeit liegen.");
        }

        // Wir erstellen IMMER einen neuen Eintrag (Damit mehrere Schichten pro Tag möglich sind!)
        DoctorSchedule newSchedule = new DoctorSchedule();
        newSchedule.setDoctor(doctor);
        newSchedule.setDayOfWeek(day);
        newSchedule.setStartTime(request.startTime());
        newSchedule.setEndTime(request.endTime());

        doctorScheduleRepository.save(newSchedule);
    }

    // Holt den kompletten Wochenplan für das Dashboard des Arztes
    public List<DoctorScheduleResponseDto> getMySchedule(String doctorEmail) {
        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));
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
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));

        doctor.setSlotDuration(minutes);
        appUserRepository.save(doctor);
    }
}