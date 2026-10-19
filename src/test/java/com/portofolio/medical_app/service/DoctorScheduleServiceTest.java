package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.ScheduleRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorSchedule;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DoctorScheduleServiceTest {

    @Mock private DoctorScheduleRepository scheduleRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private DoctorMapper doctorMapper;

    private DoctorScheduleService service;
    private AppUser doctor;
    private DoctorSchedule existing;

    @BeforeEach
    void setUp() {
        service = new DoctorScheduleService(scheduleRepository, appUserRepository, doctorMapper);

        doctor = new AppUser();
        doctor.setId(1L);
        doctor.setEmail("doc@example.com");

        existing = shift(7L, DayOfWeek.MONDAY, "09:00", "12:00");

        when(appUserRepository.findByEmail("doc@example.com")).thenReturn(Optional.of(doctor));
        when(scheduleRepository.findByDoctorId(1L)).thenReturn(List.of(existing));
        when(scheduleRepository.findById(7L)).thenReturn(Optional.of(existing));
    }

    @Test
    void savesANewShift() {
        service.saveSchedule(request("TUESDAY", "09:00", "12:00"), "doc@example.com");

        verify(scheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    void multipleShiftsOnTheSameDayAreAllowedIfTheyDoNotOverlap() {
        service.saveSchedule(request("MONDAY", "14:00", "17:00"), "doc@example.com");

        verify(scheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    void touchingShiftsAreNotAnOverlap() {
        service.saveSchedule(request("MONDAY", "12:00", "15:00"), "doc@example.com");

        verify(scheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    void overlappingShiftIsRejected() {
        ScheduleRequestDto request = request("MONDAY", "11:00", "14:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.saveSchedule(request, "doc@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("überschneidet");

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void endBeforeStartIsRejected() {
        ScheduleRequestDto request = request("TUESDAY", "12:00", "09:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.saveSchedule(request, "doc@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Endzeit");
    }

    @Test
    void zeroLengthShiftIsRejected() {
        ScheduleRequestDto request = request("TUESDAY", "09:00", "09:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.saveSchedule(request, "doc@example.com"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void invalidWeekdayIsRejectedWithBadRequestInsteadOfServerError() {
        ScheduleRequestDto request = request("FOO", "09:00", "12:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.saveSchedule(request, "doc@example.com"))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("Wochentag");
                });
    }

    @Test
    void updatingAShiftIgnoresItselfInTheOverlapCheck() {
        service.updateSchedule(7L, request("MONDAY", "09:30", "12:30"), "doc@example.com");

        assertThat(existing.getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(12, 30));
        verify(scheduleRepository).save(existing);
    }

    @Test
    void failedUpdateKeepsTheOriginalShift() {
        DoctorSchedule other = shift(8L, DayOfWeek.TUESDAY, "09:00", "12:00");
        when(scheduleRepository.findByDoctorId(1L)).thenReturn(List.of(existing, other));

        ScheduleRequestDto request = request("TUESDAY", "10:00", "11:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.updateSchedule(7L, request, "doc@example.com"))
                .isInstanceOf(AppException.class);

        assertThat(existing.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(existing.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void doctorCannotUpdateAnotherDoctorsShift() {
        ScheduleRequestDto request = request("MONDAY", "09:00", "10:00"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.updateSchedule(7L, request, "other@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void doctorCannotDeleteAnotherDoctorsShift() {
        assertThatThrownBy(() -> service.deleteSchedule(7L, "other@example.com"))
                .isInstanceOf(AppException.class);

        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void slotDurationMustBeBetween5And120Minutes() {
        assertThatThrownBy(() -> service.updateSlotDuration(4, "doc@example.com")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.updateSlotDuration(121, "doc@example.com")).isInstanceOf(AppException.class);

        service.updateSlotDuration(20, "doc@example.com");

        assertThat(doctor.getSlotDuration()).isEqualTo(20);
        verify(appUserRepository).save(doctor);
    }

    private DoctorSchedule shift(Long id, DayOfWeek day, String from, String to) {
        DoctorSchedule s = new DoctorSchedule();
        s.setId(id);
        s.setDoctor(doctor);
        s.setDayOfWeek(day);
        s.setStartTime(LocalTime.parse(from));
        s.setEndTime(LocalTime.parse(to));
        return s;
    }

    private ScheduleRequestDto request(String day, String from, String to) {
        return new ScheduleRequestDto(day, LocalTime.parse(from), LocalTime.parse(to));
    }
}