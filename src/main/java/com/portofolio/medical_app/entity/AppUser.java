package com.portofolio.medical_app.entity;

import jakarta.annotation.Nonnull; // Για το NullMarked fix
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseEntity implements UserDetails {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // Optional: Nur für Ärzte relevant (Patienten haben das leer/null)
    @Column
    private String specialty;

    // Optional: Nur für Ärzte relevant (Könnte auch für Patienten genutzt werden)
    @Column
    private String address;

    @Column
    private String phoneNumber;

    // NEU: Geschlecht (MALE, FEMALE, DIVERS) - nur für Patienten
    @Column
    private String gender;

    // NEU: Versicherungsart (PRIVATE, GESETZLICH) - nur für Patienten
    @Column
    private String insuranceType;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private int slotDuration = 30;



    // =========================================================
    // SPRING SECURITY USERDETAILS METHODEN
    // =========================================================

    @Nonnull // SonarQube / NullMarked Fix
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Nonnull // SonarQube / NullMarked Fix
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}