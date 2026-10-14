package com.portofolio.medical_app.service;


import com.portofolio.medical_app.config.Messages;
import com.portofolio.medical_app.dto.DoctorHolidayResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorHoliday;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.AppointmentRepository;
import com.portofolio.medical_app.repository.DoctorHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorHolidayService {

    private static final int MAX_HOLIDAY_DAYS = 366;

    private final DoctorHolidayRepository doctorHolidayRepository;
    private final AppUserRepository appUserRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorMapper doctorMapper;
    private final Clock clock;

    @Transactional
    public void addHolidayRange(String doctorEmail, LocalDate startDate, LocalDate endDate) {
        AppUser doctor = findDoctor(doctorEmail);

        if (endDate.isBefore(startDate)) {
            throw new AppException("Enddatum darf nicht vor Startdatum liegen.");
        }
        if (startDate.isBefore(LocalDate.now(clock))) {
            throw new AppException("Urlaub kann nicht in der Vergangenheit eingetragen werden.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) >= MAX_HOLIDAY_DAYS) {
            throw new AppException("Der Zeitraum darf höchstens " + MAX_HOLIDAY_DAYS + " Tage umfassen.");
        }

        // Bestehende Termine dürfen nicht stillschweigend überfahren werden
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime now = LocalDateTime.now(clock);
        if (from.isBefore(now)) {
            from = now;
        }
        long conflicts = appointmentRepository.countActiveInRange(
                doctor.getId(), from, endDate.plusDays(1).atStartOfDay());
        if (conflicts > 0) {
            throw new AppException("In diesem Zeitraum gibt es noch " + conflicts
                    + " aktive Termine. Bitte storniere oder verschiebe sie zuerst.");
        }

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (!doctorHolidayRepository.existsByDoctorIdAndHolidayDate(doctor.getId(), current)) {
                DoctorHoliday holiday = new DoctorHoliday();
                holiday.setDoctor(doctor);
                holiday.setHolidayDate(current);
                doctorHolidayRepository.save(holiday);
            }
            current = current.plusDays(1);
        }
    }

    public List<DoctorHolidayResponseDto> getMyHolidays(String doctorEmail) {
        AppUser doctor = findDoctor(doctorEmail);
        return doctorHolidayRepository.findByDoctorIdOrderByHolidayDateAsc(doctor.getId())
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteHoliday(Long holidayId, String doctorEmail) {
        DoctorHoliday holiday = doctorHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new AppException("Urlaub nicht gefunden.", HttpStatus.NOT_FOUND));

        if (!holiday.getDoctor().getEmail().equals(doctorEmail)) {
            throw new AppException("Du darfst nur deinen eigenen Urlaub löschen.", HttpStatus.FORBIDDEN);
        }
        doctorHolidayRepository.delete(holiday);
    }

    private AppUser findDoctor(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(Messages.DOCTOR_NOT_FOUND));
    }
}