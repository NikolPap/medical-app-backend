package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.dto.RegisterRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import org.springframework.stereotype.Component;

@Component //Sagt Spring: "Das ist ein BAustein (Bean), den du anderen Klassen zur Verfügung stellen sollst"
public class AppUserMapper {

    // Wir übergeben das DTO und das bereit verschlüsselte Password
    public AppUser toEntity(RegisterRequestDto dto, String encodePassword) {

        AppUser user = new AppUser();
        user.setEmail(dto.email());
        user.setPassword(encodePassword);
        user.setRole(dto.role());

        return user;
    }

}
