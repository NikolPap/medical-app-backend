package com.portofolio.medical_app.service;

import com.portofolio.medical_app.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String SECRET_KEY = "PortfolioMedicalAppSuperSecretKeyForJwtGenerationSpringSecurityVerificationToken";

    public String generateToken(AppUser user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());

        Instant now = Instant.now();
        Instant expiration = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .claims(extraClaims) // Νέος τρόπος (JJWT 0.12)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey()) // Η νέα έκδοση διαλέγει αυτόματα τον πιο ασφαλή αλγόριθμο
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AppUser user) {
        final String username = extractUsername(token);
        return (username.equals(user.getEmail())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(Date.from(Instant.now()));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Νέος, πολύ πιο ασφαλής τρόπος (JJWT 0.12)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}