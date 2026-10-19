package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.RegisterRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.AppUserMapper;
import com.portofolio.medical_app.mapper.VerificationTokenMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private VerificationTokenMapper tokenMapper;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(appUserRepository, passwordEncoder, new AppUserMapper(),
                tokenRepository, emailService, tokenMapper);

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("code-hash");
        when(passwordEncoder.encode("Secret1")).thenReturn("pw-hash");
        when(tokenMapper.toEntity(anyString(), any(AppUser.class))).thenReturn(new VerificationToken());
    }

    @Test
    void patientIsRegisteredWithHashedPasswordAndStaysDisabledUntilVerified() {
        service.register(patient());

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());

        AppUser user = saved.getValue();
        assertThat(user.getRole()).isEqualTo(Roles.PATIENT);
        assertThat(user.getPassword()).isEqualTo("pw-hash");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getEmail()).isEqualTo("anna@example.com");
    }

    @Test
    void doctorLandsInPendingStatus() {
        service.register(doctor());

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());

        assertThat(saved.getValue().getRole()).isEqualTo(Roles.PENDING_DOCTOR);
    }

    @Test
    void verificationCodeIsHashedInTheDatabaseAndSentAsPlainText() {
        service.register(patient());

        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq("anna@example.com"), sentCode.capture());

        assertThat(sentCode.getValue()).matches("\\d{6}");
        verify(passwordEncoder).encode(sentCode.getValue());
        verify(tokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void existingEmailIsRejected() {
        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(new AppUser()));
        RegisterRequestDto request = patient();

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("existiert bereits");

        verify(appUserRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void adminRoleCannotBeSelfAssigned() {
        RegisterRequestDto request = withRole(patient(), Roles.ADMIN);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Ungültige Rolle");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void pendingDoctorRoleCannotBeSelfAssigned() {
        RegisterRequestDto request = withRole(patient(), Roles.PENDING_DOCTOR);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    void missingRoleIsRejected() {
        RegisterRequestDto request = withRole(patient(), null);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    void doctorWithoutSpecialtyIsRejected() {
        RegisterRequestDto request = new RegisterRequestDto("doc@example.com", "Secret1", Roles.DOCTOR,
                "Max", "Muster", null, "Praxisstr. 1", "+49 611 123456", null, null);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Pflicht");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void patientWithoutInsuranceTypeIsRejected() {
        RegisterRequestDto request = new RegisterRequestDto("anna@example.com", "Secret1", Roles.PATIENT,
                "Anna", "Schmidt", null, null, "+49 611 123456", "FEMALE", null);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Pflicht");
    }

    // ---------- Helfer ----------

    private RegisterRequestDto patient() {
        return new RegisterRequestDto("anna@example.com", "Secret1", Roles.PATIENT,
                "Anna", "Schmidt", null, null, "+49 611 123456", "FEMALE", "GESETZLICH");
    }

    private RegisterRequestDto doctor() {
        return new RegisterRequestDto("doc@example.com", "Secret1", Roles.DOCTOR,
                "Max", "Muster", "Kardiologie", "Praxisstr. 1, 65183 Wiesbaden", "+49 611 123456", null, null);
    }

    private RegisterRequestDto withRole(RegisterRequestDto base, String role) {
        return new RegisterRequestDto(base.email(), base.password(), role, base.firstName(), base.lastName(),
                base.specialty(), base.address(), base.phoneNumber(), base.gender(), base.insuranceType());
    }
}