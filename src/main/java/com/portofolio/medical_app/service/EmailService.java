package com.portofolio.medical_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@medical-app.com");
            message.setTo(toEmail);
            message.setSubject("Bestätige deine E-mail-Adresse");
            message.setText("Willkommen! Dein Bestätigungscode lautet: " + token + "\nDieser Code ist 15 Minuten gültig.");

            mailSender.send(message);

            log.info("📧 E-Mail erfolgreich gesendet an: {}", toEmail);
            log.info("🔐 Der Code lautet: {}", token);
        }catch (Exception e) {
            log.error("Fehler beim Senden der E-Mail", e);
            log.warn("⚠️ FALLBACK: Der Code für {} ist: {}", toEmail, token);
        }
    }
}
