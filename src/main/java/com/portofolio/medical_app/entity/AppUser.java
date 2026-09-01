package com.portofolio.medical_app.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name= "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AppUser extends BaseEntity implements UserDetails{
    @Column(nullable = false, unique = true) //Darf nichr leer sein und jede E-mail darf es nur einmal geben
    private String email;

    @Column(nullable = false)
    private String password;

    //Später werden wir hier die Rollen definieren (z.B. Role_Admin, Role_Doctor, ROle_Patient)
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled=false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column
    private LocalDateTime lockTime;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
