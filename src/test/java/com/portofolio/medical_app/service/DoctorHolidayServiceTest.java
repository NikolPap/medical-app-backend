package com.portofolio.medical_app.service;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.DoctorHoliday;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.AppointmentRepository;
import com.portofolio.medical_app.repository.DoctorHolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DoctorHolidayServiceTest {

    // Montag, 19.10.2026, 10:00 Uhr in Berlin
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZoneId.of("Europe/Berlin"));
    private static final LocalDate START = LocalDate.of(2026, 10, 26);

    @Mock private DoctorHolidayRepository holidayRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private DoctorMapper doctorMapper;

    private DoctorHolidayService service;
    private AppUser doctor;

    @BeforeEach
    void setUp() {
        service = new DoctorHolidayService(holidayRepository, appUserRepository, appointmentRepository,
                doctorMapper, CLOCK);

        doctor = new AppUser();
        doctor.setId(1L);
        doctor.setEmail("doc@example.com");

        when(appUserRepository.findByEmail("doc@example.com")).thenReturn(Optional.of(doctor));
        when(holidayRepository.existsByDoctorIdAndHolidayDate(anyLong(), any())).thenReturn(false);
        when(appointmentRepository.countActiveInRange(anyLong(), any(), any())).thenReturn(0L);
    }

    @Test
    void savesOneEntryPerDayOfTheRange() {
        service.addHolidayRange("doc@example.com", START, START.plusDays(2));

        verify(holidayRepository, times(3)).save(any(DoctorHoliday.class));
    }

    @Test
    void daysThatAreAlreadyBlockedAreSkipped() {
        when(holidayRepository.existsByDoctorIdAndHolidayDate(1L, START.plusDays(1))).thenReturn(true);

        service.addHolidayRange("doc@example.com", START, START.plusDays(2));

        verify(holidayRepository, times(2)).save(any(DoctorHoliday.class));
    }

    @Test
    void endBeforeStartIsRejected() {
        LocalDate end = START.minusDays(1); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.addHolidayRange("doc@example.com", START, end))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Enddatum");
    }

    @Test
    void holidayInThePastIsRejected() {
        LocalDate yesterday = LocalDate.of(2026, 10, 18);

        assertThatThrownBy(() -> service.addHolidayRange("doc@example.com", yesterday, START))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Vergangenheit");
    }

    @Test
    void rangeLongerThanAYearIsRejected() {
        LocalDate end = START.plusDays(400); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.addHolidayRange("doc@example.com", START, end))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("höchstens");

        verify(holidayRepository, never()).save(any());
    }

    @Test
    void existingActiveAppointmentsBlockTheHoliday() {
        when(appointmentRepository.countActiveInRange(anyLong(), any(), any())).thenReturn(2L);

        LocalDate end = START.plusDays(2); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.addHolidayRange("doc@example.com", START, end))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("2 aktive Termine");

        verify(holidayRepository, never()).save(any());
    }

    @Test
    void doctorCannotDeleteAnotherDoctorsHoliday() {
        DoctorHoliday holiday = new DoctorHoliday();
        holiday.setDoctor(doctor);
        holiday.setHolidayDate(START);
        when(holidayRepository.findById(5L)).thenReturn(Optional.of(holiday));

        assertThatThrownBy(() -> service.deleteHoliday(5L, "other@example.com"))
                .isInstanceOf(AppException.class);

        verify(holidayRepository, never()).delete(any());
    }
}