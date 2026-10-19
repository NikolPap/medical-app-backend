package com.portofolio.medical_app.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING,   CONFIRMED, true",
            "PENDING,   CANCELLED, true",
            "PENDING,   PENDING,   false",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, PENDING,   false",
            "CONFIRMED, CONFIRMED, false",
            "CANCELLED, PENDING,   false",
            "CANCELLED, CONFIRMED, false",
            "CANCELLED, CANCELLED, false"
    })
    void transitionsFollowTheStateMachine(AppointmentStatus from, AppointmentStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void cancelledAppointmentsCanNeverBeRevived() {
        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(AppointmentStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }
}