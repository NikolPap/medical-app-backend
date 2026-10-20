package com.portofolio.medical_app.controller;

import com.portofolio.medical_app.config.JwtAuthenticationFilter;
import com.portofolio.medical_app.config.SecurityConfig;
import com.portofolio.medical_app.exception.GlobalExceptionHandler;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.service.AdminService;
import com.portofolio.medical_app.service.AppointmentService;
import com.portofolio.medical_app.service.DoctorScheduleService;
import com.portofolio.medical_app.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminController.class, AppointmentController.class, DoctorScheduleController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SecurityRulesTest {

    @Autowired private MockMvc mockMvc;

    // Abhängigkeiten der Controller und des JWT-Filters
    @MockitoBean private AdminService adminService;
    @MockitoBean private AppointmentService appointmentService;
    @MockitoBean private DoctorScheduleService doctorScheduleService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AppUserRepository appUserRepository;

    // ---------- Nicht eingeloggt ----------

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/appointments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bitte melde dich an."));
    }

    @Test
    void garbageBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/appointments/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void availableSlotsArePublic() throws Exception {
        when(appointmentService.getAvailableTimeSlots(1L, java.time.LocalDate.of(2026, 10, 26)))
                .thenReturn(List.of("09:00"));

        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("doctorId", "1").param("date", "2026-10-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("09:00"));
    }

    // ---------- Admin-Bereich ----------

    @Test
    void adminEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/doctors/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientCannotListPendingDoctors() throws Exception {
        mockMvc.perform(get("/api/admin/doctors/pending").with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    void doctorCannotListPendingDoctors() throws Exception {
        mockMvc.perform(get("/api/admin/doctors/pending").with(user("doc").authorities(() -> "ROLE_DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingDoctorCannotListPendingDoctors() throws Exception {
        mockMvc.perform(get("/api/admin/doctors/pending").with(user("new").authorities(() -> "ROLE_PENDING_DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotApproveADoctor() throws Exception {
        mockMvc.perform(put("/api/admin/doctors/5/approve").with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    void adminCanListPendingDoctors() throws Exception {
        when(adminService.getPendingDoctors()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/doctors/pending").with(user("admin").authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void adminCanApproveADoctor() throws Exception {
        mockMvc.perform(put("/api/admin/doctors/5/approve").with(user("admin").authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());

        verify(adminService).approveDoctor(5L);
    }

    // ---------- Arzt-Bereich ----------

    @Test
    void patientCannotReadDoctorSchedule() throws Exception {
        mockMvc.perform(get("/api/schedules/me").with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(doctorScheduleService);
    }

    @Test
    void doctorCanReadHisSchedule() throws Exception {
        when(doctorScheduleService.getMySchedule("doc")).thenReturn(List.of());

        mockMvc.perform(get("/api/schedules/me").with(user("doc").authorities(() -> "ROLE_DOCTOR")))
                .andExpect(status().isOk());
    }

    @Test
    void patientCannotChangeSlotDuration() throws Exception {
        mockMvc.perform(put("/api/schedules/duration").param("minutes", "20")
                        .with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(doctorScheduleService);
    }

    // ---------- Termine: Rollen und Validierung ----------

    @Test
    void pendingDoctorCannotBookAppointments() throws Exception {
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"appointmentDate\":\"2026-10-26\",\"appointmentTime\":\"09:00\"}")
                        .with(user("new").authorities(() -> "ROLE_PENDING_DOCTOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appointmentService);
    }

    @Test
    void bookingWithoutDoctorIdReturns400WithValidationMessage() throws Exception {
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentDate\":\"2026-10-26\",\"appointmentTime\":\"09:00\"}")
                        .with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte wähle einen Arzt."));

        verifyNoInteractions(appointmentService);
    }

    @Test
    void bookingWithInvalidTimeReturns400() throws Exception {
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"appointmentDate\":\"2026-10-26\",\"appointmentTime\":\"25:99\"}")
                        .with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ungültige Uhrzeit."));
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        mockMvc.perform(put("/api/appointments/10/status").param("status", "FOO")
                        .with(user("anna").authorities(() -> "ROLE_PATIENT")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }
}