package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.ScheduleRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorSchedule;
import com.portofolio.medical_app.exception.AppException;
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

    // Speichert oder aktualisiert den Arbeitsplan für einen Tag
    // Speichert EINE NEUE Arbeitszeit (Schicht) für einen Tag
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
    public List<DoctorSchedule> getMySchedule(String doctorEmail) {
        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));

        return doctorScheduleRepository.findByDoctorId(doctor.getId());
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
}