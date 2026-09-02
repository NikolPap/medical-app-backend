package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("SELECT p FROM PasswordResetToken p JOIN p.user u WHERE u.email = :email AND p.used = false")
    Optional<PasswordResetToken> findValidTokenByEmail(@Param("email") String email);

}
