package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Wir brauchen das späater dringend für das Spring Security (Login).
    Optional<AppUser> findByEmail(final String email);
}
