package com.portofolio.medical_app.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private static final String SENDER_EMAIL = "noreply@medical-app.com";

    private final JavaMailSender mailSender;

    // Προστέθηκε ο "διακόπτης". Αν είμαστε σε παραγωγή (prod), αυτό θα είναι false.
    @Value("${app.mail.log-codes:false}")
    private boolean logCodes;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Bestätige deine E-mail-Adresse");
            message.setText("Willkommen! Dein Bestätigungscode lautet: " + token + "\nDieser Code ist 15 Minuten gültig.");

            mailSender.send(message);

            LOGGER.info("Verifizierungs-Mail gesendet.");

            // Τυπώνει τον κωδικό μόνο αν επιτρέπεται (dev)
            if (logCodes) {
                LOGGER.info("DEV: Code für {} lautet {}", toEmail, token);
            }

        } catch (Exception e) {
            LOGGER.error("Fehler beim Senden der Verifizierungs-Mail: {}", e.getMessage());

            if (logCodes) {
                LOGGER.warn("DEV-FALLBACK: Code für {} ist {}", toEmail, token);
            }
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Reset Password");
            message.setText("Du hast eine Anfrage zum Zurücksetzen deines Passworts gestellt.\n" +
                    "Dein Code lautet: " + token + "\n" +
                    "Dieser Code ist 15 Minuten gültig.");

            mailSender.send(message);

            LOGGER.info("Passwort-Reset-E-Mail gesendet.");

            if (logCodes) {
                LOGGER.info("DEV: Passwort-Reset-Code für {} lautet {}", toEmail, token);
            }
        } catch (Exception e) {
            LOGGER.error("Fehler beim Senden der Passwort-Reset-E-Mail: {}", e.getMessage());

            if (logCodes) {
                LOGGER.warn("DEV-FALLBACK: Der Passwort-Reset-Code für {} ist: {}", toEmail, token);
            }
        }
    }

    public void sendStatusEmail(String toEmail, String messageBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Termin-Update");
            message.setText(messageBody);

            mailSender.send(message);

            LOGGER.info("Status-Update gesendet.");

            if (logCodes) {
                LOGGER.info("DEV: Status-Update an {} gesendet: {}", toEmail, messageBody);
            }
        } catch (Exception e) {
            LOGGER.error("Fehler beim Senden der Status-Update-E-Mail: {}", e.getMessage());

            if (logCodes) {
                LOGGER.warn("DEV-FALLBACK EMAIL an {}: {}", toEmail, messageBody);
            }
        }
    }
}