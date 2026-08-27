package com.portofolio.medical_app.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appoitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime appoitmentDate; //Datum und Uhrzeit des Termins

    @Column(nullable = false)
    private String status; //z.B. "PENDING" (Ausstehend"), "CONFIRMED",(Bestädigt), "CANCELLED"(Storniert)

    // ===================================
    // BEZIUNGEN ZU ANDEREN TABELLEB (Foreign Keys)
    // ===================================

    //
    @ManyToOne //Viele Termine können zu Einem Patienten ghören
    @JoinColumn(name = "patient_id", nullable = false) //Das erstellt die Spalte "patient_id" in der Datenbank
    private AppUser patient;

    //Welcher Arzt ist züstandig?
    @ManyToOne // Viele Termine könnem zu Einem Arzt gehören
    @JoinColumn(name = "doctor_id", nullable = false) // Das erstellt die Spalte "doctor_id" in der DAtenbank
    private AppUser doctor;
}
