package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.PendingDoctorResponseDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.DoctorMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private DoctorMapper doctorMapper;
    @Mock private EmailService emailService;

    private AdminService service;
    private AppUser pending;

    @BeforeEach
    void setUp() {
        service = new AdminService(appUserRepository, doctorMapper, emailService);

        pending = new AppUser();
        pending.setId(5L);
        pending.setEmail("new.doc@example.com");
        pending.setRole(Roles.PENDING_DOCTOR);
    }

    @Test
    void approveChangesRoleToDoctorAndSendsMail() {
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(pending));

        service.approveDoctor(5L);

        assertThat(pending.getRole()).isEqualTo(Roles.DOCTOR);
        verify(appUserRepository).save(pending);
        verify(emailService).sendStatusEmail(any(), any());
    }

    @Test
    void rejectChangesRoleToRejectedDoctor() {
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(pending));

        service.rejectDoctor(5L);

        assertThat(pending.getRole()).isEqualTo(Roles.REJECTED_DOCTOR);
        verify(appUserRepository).save(pending);
    }

    @Test
    void approvingAUserThatIsNotPendingIsRejected() {
        pending.setRole(Roles.PATIENT);
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approveDoctor(5L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("wartet nicht auf Freischaltung");

        assertThat(pending.getRole()).isEqualTo(Roles.PATIENT);
        verify(appUserRepository, never()).save(any());
        verify(emailService, never()).sendStatusEmail(any(), any());
    }

    @Test
    void approvingAnAlreadyApprovedDoctorIsRejected() {
        pending.setRole(Roles.DOCTOR);
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approveDoctor(5L)).isInstanceOf(AppException.class);
    }

    @Test
    void unknownUserResultsInNotFound() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveDoctor(99L))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void pendingListIsMappedToDtos() {
        PendingDoctorResponseDto dto = new PendingDoctorResponseDto(
                5L, "new.doc@example.com", "Anna", "Schmidt", "Kardiologie", "Straße 1", "+49 611 123456");
        when(appUserRepository.findByRoleAndEnabledTrue(Roles.PENDING_DOCTOR)).thenReturn(List.of(pending));
        when(doctorMapper.toPendingDto(pending)).thenReturn(dto);

        assertThat(service.getPendingDoctors()).containsExactly(dto);
    }
}