package com.portofolio.medical_app.service;

import com.portofolio.medical_app.entity.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Das ist unser geheimer Schlüssel! Wenn ein Hacker den nicht kennt,
    // kann er keine falschen Tokens herstellen.
    // WICHTIG: Muss mindestens 256-bit (32 Zeichen) lang sein!
    private static final String SECRET_KEY = "PortfolioMedicalAppSuperSecretKeyForJwtGeneration";

    // Diese Methode generiert den eigentlichen Token
    public String generateToken(AppUser user) {

        // Extra-Daten (Claims), die wir im Token speichern wollen (z.B. die Rolle)
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());

        return Jwts.builder()
                .setClaims(extraClaims) // Unsere Extra-Daten
                .setSubject(user.getEmail()) // Wer ist der Besitzer? (E-Mail)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Wann wurde der Token erstellt?
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Läuft ab in 24 Stunden
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Mit unserem Geheimnis unterschreiben
                .compact(); // Alles zu einem langen String (Token) zusammenpressen
    }

    // Hilfsmethode, um unseren String-Schlüssel in einen echten kryptografischen Key zu verwandeln
    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}