# MedSync – Medical Appointment Platform (Backend)

![CI](https://github.com/NikolPap/medical-app-backend/actions/workflows/ci.yml/badge.svg)

Spring Boot REST-API für die Terminverwaltung von Arztpraxen.
Frontend: [medical-app-frontend](https://github.com/NikolPap/medical-app-frontend)

> **Demo-Projekt.** Keine echten Patientendaten eingeben.

## Features
- Rollen: Patient, Arzt (mit Admin-Freischaltung), Admin
- Stateless JWT-Authentifizierung mit eigenem Filter, BCrypt, Brute-Force-Schutz
- E-Mail-Verifizierung und Passwort-Reset mit gehashten, zeitlich begrenzten Codes
- Verfügbarkeits-Engine: freie Slots aus Schichtplänen, Urlaub und bestehenden Buchungen
- Statusmaschine für Termine, 48-Stunden-Stornoregel, atomares Verschieben (`@Transactional`)
- Schutz vor Doppelbuchung: Service-Prüfung plus partieller Unique-Index in PostgreSQL

## Tech-Stack
Java 21 · Spring Boot 4 · Spring Security · Spring Data JPA/Hibernate · PostgreSQL · JJWT ·
Bean Validation · JUnit 5, Mockito, MockMvc, Testcontainers · JaCoCo · Docker · GitHub Actions

## Architektur
Controller → Service → Repository → Entity, DTOs und Mapper an den Schichtgrenzen,
zentraler `GlobalExceptionHandler`.

## Starten
```bash
cp .env.example .env      # Werte anpassen
docker compose up --build
```
- API: http://localhost:8080
- E-Mails (MailHog): http://localhost:8025
- Mit Frontend: `docker compose -f docker-compose.full.yml up --build` → http://localhost:4200

## Tests
```bash
./mvnw verify    # Unit-, Security- und Integrationstests (Docker erforderlich)
```
Coverage-Report: `target/site/jacoco/index.html`

## Entscheidungen
- **JWT statt Session:** stateless, passend zur SPA. Token im localStorage (XSS-Risiko bekannt;
  Alternative: httpOnly-Cookie).
- **Partieller Unique-Index** zusätzlich zur Service-Prüfung, weil Prüfen und Speichern nicht atomar sind.
- **Fixed `Clock`** in Services, damit Zeitlogik deterministisch testbar ist.