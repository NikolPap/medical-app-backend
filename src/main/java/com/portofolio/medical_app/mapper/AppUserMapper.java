package com.portofolio.medical_app.mapper;

import com.portofolio.medical_app.config.Roles;
import com.portofolio.medical_app.dto.RegisterRequestDto;
import com.portofolio.medical_app.entity.AppUser;
import org.springframework.stereotype.Component;

@Component //Sagt Spring: "Das ist ein BAustein (Bean), den du anderen Klassen zur Verfügung stellen sollst"
public class AppUserMapper {

    // Wir übergeben das DTO und das bereit verschlüsselte Password
    public AppUser toEntity(RegisterRequestDto dto, String encodedPassword) {
        AppUser user = new AppUser();
        user.setEmail(dto.email());
        user.setPassword(encodedPassword);
        user.setRole(dto.role());

        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());

        // Prüfen, ob der Nutzer ein Arzt ist
        boolean isDoctor = Roles.DOCTOR.equals(dto.role());

        // Ärzte haben Fachrichtung/Adresse, Patienten nicht
        user.setSpecialty(isDoctor ? blankToNull(dto.specialty()) : null);
        user.setAddress(isDoctor ? blankToNull(dto.address()) : null);

        // Patienten haben Geschlecht/Versicherung, Ärzte nicht
        user.setGender(isDoctor ? null : blankToNull(dto.gender()));
        user.setInsuranceType(isDoctor ? null : blankToNull(dto.insuranceType()));

        return user;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}