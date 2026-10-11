package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.dto.DoctorHolidayResponseDto;
import com.portofolio.medical_app.dto.DoctorResponseDto;
import com.portofolio.medical_app.dto.DoctorScheduleResponseDto;
import com.portofolio.medical_app.dto.PendingDoctorResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorHoliday;
import com.portofolio.medical_app.entity.DoctorSchedule;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {
    public DoctorResponseDto toDto(AppUser doctor) {
        return new DoctorResponseDto(
                doctor.getId(),
                doctor.getEmail(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getSpecialty(),
                doctor.getAddress(),
                doctor.getSlotDuration()
        );
    }

    public DoctorScheduleResponseDto toDto(DoctorSchedule schedule) {
        return new DoctorScheduleResponseDto(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }

    public DoctorHolidayResponseDto toDto(DoctorHoliday holiday) {
        return new DoctorHolidayResponseDto(holiday.getId(), holiday.getHolidayDate());
    }

    public PendingDoctorResponseDto toPendingDto(AppUser user) {
        return new PendingDoctorResponseDto(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getSpecialty(), user.getAddress(), user.getPhoneNumber()
        );
    }
}
