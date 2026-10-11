package com.portofolio.medical_app.config;

import com.portofolio.medical_app.entity.AppUser;
import com.portofolio.medical_app.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;



@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            LOGGER.info("Kein Admin konfiguriert (ADMIN_EMAIL / ADMIN_PASSWORD fehlen).");
            return;
        }
        if (appUserRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstName("Admin");
        admin.setLastName("MedSync");
        admin.setRole(Roles.ADMIN);
        admin.setEnabled(true);
        appUserRepository.save(admin);

        LOGGER.info("Admin-Account wurde angelegt.");
    }
}