package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class VerificationTokenMapper {

    private final Clock clock;

    public VerificationToken toEntity(final String hashedToken, final AppUser user) {
        final VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(hashedToken);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));
        return verificationToken;
    }
}