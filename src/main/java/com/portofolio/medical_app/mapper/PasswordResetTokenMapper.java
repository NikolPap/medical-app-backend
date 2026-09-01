package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.PasswordResetToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PasswordResetTokenMapper {
    public PasswordResetToken toEntity(String hashedToken, AppUser user) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(hashedToken);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        return resetToken;
    }
}
