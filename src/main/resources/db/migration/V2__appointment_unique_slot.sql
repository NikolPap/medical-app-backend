CREATE UNIQUE INDEX uq_appointments_doctor_slot
    ON appointments (doctor_id, appointment_date)
    WHERE status <> 'CANCELLED';

CREATE INDEX idx_appointments_patient ON appointments (patient_id);
CREATE INDEX idx_holidays_doctor_date ON doctor_holidays (doctor_id, holiday_date);