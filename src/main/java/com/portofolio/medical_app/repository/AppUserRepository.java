package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    // Wir brauchen das späater dringend für das Spring Security (Login).
    Optional<AppUser> findByEmail(String email);
}
