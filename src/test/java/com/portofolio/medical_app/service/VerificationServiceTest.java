package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.VerificationRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import com.portofolio.medical_app.exception.AppException;
import com.portofolio.medical_app.repository.AppUserRepository;
import com.portofolio.medical_app.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T08:00:00Z"), ZoneId.of("Europe/Berlin"));

    @Mock private AppUserRepository appUserRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private VerificationService service;
    private AppUser user;
    private VerificationToken token;
    private VerificationRequestDto request;

    @BeforeEach
    void setUp() {
        service = new VerificationService(appUserRepository, tokenRepository, passwordEncoder, CLOCK);

        user = new AppUser();
        user.setEmail("anna@example.com");
        user.setEnabled(false);

        token = new VerificationToken();
        token.setToken("hashed-code");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now(CLOCK).plusMinutes(10));

        request = new VerificationRequestDto("anna@example.com", "123456");

        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser_EmailAndUsedFalse("anna@example.com")).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);
    }

    @Test
    void correctCodeEnablesTheAccountAndConsumesTheToken() {
        service.verifyAccount(request);

        assertThat(user.isEnabled()).isTrue();
        assertThat(token.isUsed()).isTrue();
        verify(appUserRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void unknownUserIsRejected() {
        when(appUserRepository.findByEmail("anna@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAccount(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Benutzer nicht gefunden");
    }

    @Test
    void alreadyVerifiedAccountIsRejected() {
        user.setEnabled(true);

        assertThatThrownBy(() -> service.verifyAccount(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("bereits verifiziert");
    }

    @Test
    void missingTokenIsRejected() {
        when(tokenRepository.findByUser_EmailAndUsedFalse("anna@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAccount(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Kein gültiger Bestätigungscode");
    }

    @Test
    void expiredCodeIsRejectedAndAccountStaysDisabled() {
        token.setExpiresAt(LocalDateTime.now(CLOCK).minusMinutes(1));

        assertThatThrownBy(() -> service.verifyAccount(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("abgelaufen");

        assertThat(user.isEnabled()).isFalse();
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void wrongCodeCountsAttemptsAndKeepsAccountDisabled() {
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyAccount(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Noch 4 Versuche");

        assertThat(token.getFailedAttempts()).isEqualTo(1);
        assertThat(token.isUsed()).isFalse();
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void fifthWrongAttemptInvalidatesTheCode() {
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(false);
        token.setFailedAttempts(4);

        assertThatThrownBy(() -> service.verifyAccount(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Zu viele Fehlversuche");

        assertThat(token.isUsed()).isTrue();
        assertThat(user.isEnabled()).isFalse();
    }
}