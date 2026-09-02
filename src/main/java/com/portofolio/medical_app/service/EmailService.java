package com.portofolio.medical_app.service;

import com.portofolio.medical_app.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@medical-app.com");
            message.setTo(toEmail);
            message.setSubject("Bestätige deine E-mail-Adresse");
            message.setText("Willkommen! Dein Bestätigungscode lautet: " + token + "\nDieser Code ist 15 Minuten gültig.");

            mailSender.send(message);

            LOGGER.info("E-Mail adresse: {}", toEmail);

            System.out.println("🔐 Der Code lautet: {}" + token);

        } catch (Exception e) {
            LOGGER.error("Fehler beim Senden der E-Mail: {}", e.getMessage());
            LOGGER.warn("⚠️ FALLBACK: Der Code für {} ist: {}", toEmail, token);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@medical-app.com");
            message.setTo(toEmail);
            message.setSubject("Reset Password");
            message.setText("Du hast eine Anfrage zum Zurücksetzen deines Passworts gestellt.\n" +
                    "Dein Code lautet: " + token + "\n" +
                    "Dieser Code ist 15 Minuten gültig.");
            mailSender.send(message);
            LOGGER.info("📧 Passwort-Reset-E-Mail erfolgreich gesendet an: {}", toEmail);
        } catch (Exception e) {
            LOGGER.warn("⚠️ FALLBACK: Der Passwort-Reset-Code für {} ist: {}", toEmail, token);
        }
    }

}