package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.DoctorHolidayResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorHoliday;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.DoctorHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorHolidayService {

    private final DoctorHolidayRepository doctorHolidayRepository;
    private final AppUserRepository appUserRepository;
    private final DoctorMapper doctorMapper;

    // NEU: Speichert eine ganze Range (z.B. 1. Juli bis 15. Juli)
    public void addHolidayRange(String doctorEmail, LocalDate startDate, LocalDate endDate) {
        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));

        if (endDate.isBefore(startDate)) {
            throw new AppException("Enddatum darf nicht vor Startdatum liegen.");
        }

        // Wir loopen durch jeden einzelnen Tag vom Start bis zum Ende!
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Nur speichern, wenn dieser spezifische Tag nicht schon drin ist
            if (!doctorHolidayRepository.existsByDoctorIdAndHolidayDate(doctor.getId(), currentDate)) {
                DoctorHoliday holiday = new DoctorHoliday();
                holiday.setDoctor(doctor);
                holiday.setHolidayDate(currentDate);
                doctorHolidayRepository.save(holiday);
            }
            currentDate = currentDate.plusDays(1); // Nächster Tag
        }
    }

    public List<DoctorHolidayResponseDto> getMyHolidays(String doctorEmail) {
        AppUser doctor = appUserRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new AppException("Arzt nicht gefunden."));
        return doctorHolidayRepository.findByDoctorIdOrderByHolidayDateAsc(doctor.getId())
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    public void deleteHoliday(Long holidayId, String doctorEmail) {
        DoctorHoliday holiday = doctorHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new AppException("Urlaub nicht gefunden."));

        if (!holiday.getDoctor().getEmail().equals(doctorEmail)) {
            throw new AppException("Du darfst nur deinen eigenen Urlaub löschen.");
        }
        doctorHolidayRepository.delete(holiday);
    }
}