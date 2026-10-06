package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenMapper {

    private final Clock clock;

    public PasswordResetToken toEntity(final String hashedToken, final AppUser user) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(hashedToken);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now(clock).plusMinutes(15));
        return resetToken;
    }
}