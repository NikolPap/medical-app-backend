package com.portofolio.medical_app.repository;

import com.portofolio.medical_app.entity.VerificationToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByUser_EmailAndUsedFalse(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken  v WHERE v.used = false  AND v.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

}
