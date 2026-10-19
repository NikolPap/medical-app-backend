package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.ForgotPasswordRequestDto;
import com.portofolio.medical_app.dto.ResetPasswordRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.PasswordResetToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.mapper.PasswordResetTokenMapper;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZONE);

    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private PasswordResetTokenMapper tokenMapper;

    private PasswordResetService service;
    private PasswordResetToken token;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                appUserRepository, tokenRepository, passwordEncoder, emailService, tokenMapper, CLOCK);

        user = new AppUser();
        user.setEmail("anna@example.com");
        user.setAccountNonLocked(false);
        user.setFailedLoginAttempts(5);

        token = new PasswordResetToken();
        token.setToken("hashed-code");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now(CLOCK).plusMinutes(10));

        when(tokenRepository.findValidTokenByEmail("anna@example.com")).thenReturn(Optional.of(token));
    }

    @Test
    void correctCodeResetsPasswordAndUnlocksAccount() {
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequestDto("anna@example.com", "123456", "NewPass1"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(token.isUsed()).isTrue();
        verify(appUserRepository).save(user);
    }

    @Test
    void expiredCodeIsRejected() {
        token.setExpiresAt(LocalDateTime.now(CLOCK).minusMinutes(1));

        assertThatThrownBy(() -> service.verifyResetCode("anna@example.com", "123456"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("abgelaufen");
    }

    @Test
    void wrongCodeIncrementsFailedAttemptsAndReportsRemaining() {
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyResetCode("anna@example.com", "000000"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Noch 4 Versuche");

        assertThat(token.getFailedAttempts()).isEqualTo(1);
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void fifthWrongAttemptInvalidatesTheCode() {
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        token.setFailedAttempts(4);

        assertThatThrownBy(() -> service.verifyResetCode("anna@example.com", "000000"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Zu viele Fehlversuche");

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void unknownEmailHasNoValidCode() {
        when(tokenRepository.findValidTokenByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyResetCode("ghost@example.com", "123456"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Kein gültiger Reset-Code");
    }

    // ---------- requestPasswordReset ----------

    @Test
    void unknownEmailSilentlyDoesNothing() {
        when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("ghost@example.com");

        service.requestPasswordReset(request);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void unverifiedAccountSilentlyDoesNothing() {
        user.setEnabled(false);
        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("anna@example.com");

        service.requestPasswordReset(request);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void validRequestInvalidatesOldCodesStoresHashedCodeAndSendsPlainCode() {
        user.setId(1L);
        user.setEnabled(true);
        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("code-hash");
        when(tokenMapper.toEntity("code-hash", user)).thenReturn(token);
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("anna@example.com");

        service.requestPasswordReset(request);

        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).invalidateAllActiveTokens(1L);
        verify(tokenRepository).save(token);
        verify(emailService).sendPasswordResetEmail(eq("anna@example.com"), sentCode.capture());

        // Per Mail geht der Klartext-Code raus, in der Datenbank landet nur der Hash
        assertThat(sentCode.getValue()).matches("\\d{6}");
        verify(passwordEncoder).encode(sentCode.getValue());
    }
}