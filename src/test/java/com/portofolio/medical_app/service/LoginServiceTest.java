package com.portofolio.medical_app.service;

import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.AuthResponseDto;
import com.portofolio.medical_app.dto.LoginRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private LoginService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new LoginService(appUserRepository, passwordEncoder, jwtService);

        user = new AppUser();
        user.setEmail("anna@example.com");
        user.setPassword("hash");
        user.setRole(Roles.PATIENT);
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Correct1", "hash")).thenReturn(true);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
    }

    @Test
    void successfulLoginReturnsTokenAndRole() {
        AuthResponseDto response = service.login(new LoginRequestDto("anna@example.com", "Correct1"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo(Roles.PATIENT);
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        user.setFailedLoginAttempts(3);

        service.login(new LoginRequestDto("anna@example.com", "Correct1"));

        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(appUserRepository).save(user);
    }

    @Test
    void unknownEmailGivesUnauthorizedWithGenericMessage() {
        when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        LoginRequestDto request = new LoginRequestDto("ghost@example.com", "x"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).contains("Falsche E-Mail oder Passwort");
                });
    }

    @Test
    void unverifiedAccountCannotLogIn() {
        user.setEnabled(false);

        LoginRequestDto request = new LoginRequestDto("anna@example.com", "Correct1"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void lockedAccountCannotLogInEvenWithCorrectPassword() {
        user.setAccountNonLocked(false);

        LoginRequestDto request = new LoginRequestDto("anna@example.com", "Correct1"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("gesperrt");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void wrongPasswordIncrementsCounterWithoutLocking() {
        LoginRequestDto request = new LoginRequestDto("anna@example.com", "wrong"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(AppException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    void fifthWrongPasswordLocksTheAccount() {
        user.setFailedLoginAttempts(4);

        LoginRequestDto request = new LoginRequestDto("anna@example.com", "wrong"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Zu viele Fehlversuche");

        assertThat(user.isAccountNonLocked()).isFalse();
    }

    @Test
    void pendingDoctorCannotLogInEvenWithCorrectPassword() {
        user.setRole(Roles.PENDING_DOCTOR);

        LoginRequestDto request = new LoginRequestDto("anna@example.com", "Correct1"); // Αλλαγή: Βγήκε έξω

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getMessage()).contains("geprüft");
                });

        verify(jwtService, never()).generateToken(any());
    }
}