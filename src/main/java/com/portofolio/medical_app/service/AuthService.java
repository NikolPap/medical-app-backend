package com.portofolio.medical_app.service;

import com.portofolio.medical_app.dto.RegisterRequest;
import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    //Die Methode zur Registrierung
    public AppUser register(RegisterRequest request) {
        //1. Prüfen: Gibt es diese E-mail schon in der Datenbank?
        if(appUserRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Diese Email existiert bereits!"); //Bricht den Vorgang mit Fehler ab
        }

        //2. Einen neuen Benutzer (Entity) erstellen
        AppUser newUser = new AppUser();
        newUser.setEmail(request.email());

        //3. Das Passwort verschlüsseln, BEVOR wir es speichern!(Sehr wichtig)
        newUser.setPassword(passwordEncoder.encode(request.password()));

        //4. Rolle setzen
        newUser.setRole(request.role());

        //5. In der PostgreSQL DAtenbank speichern und den gespeicherten Benutzer zurückgeben
        return appUserRepository.save(newUser);


    }
}
