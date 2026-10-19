package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.dto.PendingDoctorResponseDto;
import com.portofolio.medical_app.dto.RegisterResponseDto;
import com.portofolio.medical_app.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @InjectMocks private AdminController adminController;

    @Test
    void getPendingDoctorsReturnsOkWithTheServiceResult() {
        List<PendingDoctorResponseDto> doctors = List.of(
                doctor(1L, "Anna", "Schmidt", "Kardiologie"),
                doctor(2L, "Max", "Müller", "Dermatologie"));
        when(adminService.getPendingDoctors()).thenReturn(doctors);

        ResponseEntity<List<PendingDoctorResponseDto>> response = adminController.getPendingDoctors();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(doctors);
    }

    @Test
    void getPendingDoctorsReturnsEmptyListWhenNobodyIsWaiting() {
        when(adminService.getPendingDoctors()).thenReturn(List.of());

        ResponseEntity<List<PendingDoctorResponseDto>> response = adminController.getPendingDoctors();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void approveDelegatesToTheService() {
        ResponseEntity<RegisterResponseDto> response = adminController.approve(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).approveDoctor(5L);
    }

    @Test
    void rejectDelegatesToTheService() {
        ResponseEntity<RegisterResponseDto> response = adminController.reject(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).rejectDoctor(5L);
    }

    private static PendingDoctorResponseDto doctor(Long id, String firstName, String lastName, String specialty) {
        return new PendingDoctorResponseDto(
                id,
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com",
                firstName,
                lastName,
                specialty,
                "Musterstraße 1, 65183 Wiesbaden",
                "+49 611 123456");
    }
}