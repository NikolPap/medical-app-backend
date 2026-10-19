package com.portofolio.medical_app.service;

import com.portofolio.medical_app.entity.AppUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private JwtService jwtService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        jwtService = newService(SECRET, 24);

        user = new AppUser();
        user.setEmail("anna@example.com");
        user.setRole("ROLE_PATIENT");
    }

    private JwtService newService(String secret, long hours) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "expirationHours", hours);
        service.init();
        return service;
    }

    @Test
    void generatedTokenContainsTheEmailAsSubject() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("anna@example.com");
    }

    @Test
    void validTokenIsAcceptedForItsOwner() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsRejectedForAnotherUser() {
        String token = jwtService.generateToken(user);

        AppUser other = new AppUser();
        other.setEmail("other@example.com");

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = newService(SECRET, -1);
        String token = shortLived.generateToken(user);

        assertThatThrownBy(() -> shortLived.extractUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenWithReplacedSignatureIsRejected() {
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService foreign = newService("another-secret-another-secret-another-123", 24);
        String token = foreign.generateToken(user);

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }
}