package com.portofolio.medical_app.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name= "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)//Auto-Increment (1,2 , 3....)
    private Long id;

    @Column(nullable = false, unique = true) //Darf nichr leer sein und jede E-mail darf es nur einmal geben
    private String email;

    @Column(nullable = false)
    private String password;

    //Später werden wir hier die Rollen definieren (z.B. Role_Admin, Role_Doctor, ROle_Patient)
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled=false;

}
