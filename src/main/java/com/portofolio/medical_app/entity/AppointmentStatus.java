package com.portofolio.medical_app.entity;

import lombok.Getter;

import java.util.EnumSet;

@Getter
public enum AppointmentStatus {
    PENDING("ausstehend"),
    CONFIRMED("bestätigt"),
    CANCELLED("storniert");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    // Erlaubt: PENDING -> CONFIRMED/CANCELLED, CONFIRMED -> CANCELLED
    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED).contains(target);
            case CONFIRMED -> EnumSet.of(CANCELLED).contains(target);
            case CANCELLED -> false;
        };
    }
}
