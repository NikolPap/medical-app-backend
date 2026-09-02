package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.entity.VerificationToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VerificationTokenMapper {

    public VerificationToken toEntity(final String hashedToken,
                                      final AppUser user) {
       final VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(hashedToken); //Hier kommt der verschlüsselte Code rein!
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        return verificationToken;
    }

}
