CREATE TABLE app_users (
                           id                    BIGSERIAL PRIMARY KEY,
                           email                 VARCHAR(255) NOT NULL UNIQUE,
                           password              VARCHAR(255) NOT NULL,
                           first_name            VARCHAR(255) NOT NULL,
                           last_name             VARCHAR(255) NOT NULL,
                           specialty             VARCHAR(255),
                           address               VARCHAR(255),
                           phone_number          VARCHAR(255),
                           gender                VARCHAR(255),
                           insurance_type        VARCHAR(255),
                           role                  VARCHAR(255) NOT NULL,
                           enabled               BOOLEAN NOT NULL,
                           failed_login_attempts INTEGER NOT NULL,
                           account_non_locked    BOOLEAN NOT NULL,
                           slot_duration         INTEGER NOT NULL
);

CREATE TABLE appointments (
                              id               BIGSERIAL PRIMARY KEY,
                              appointment_date TIMESTAMP(6) NOT NULL,
                              status           VARCHAR(255) NOT NULL,
                              duration_minutes INTEGER NOT NULL DEFAULT 30,
                              patient_id       BIGINT NOT NULL REFERENCES app_users (id),
                              doctor_id        BIGINT NOT NULL REFERENCES app_users (id)
);

CREATE TABLE doctor_schedules (
                                  id          BIGSERIAL PRIMARY KEY,
                                  doctor_id   BIGINT NOT NULL REFERENCES app_users (id),
                                  day_of_week SMALLINT NOT NULL,   -- DayOfWeek ohne @Enumerated = Ordinal (MONDAY=0)
                                  start_time  TIME NOT NULL,
                                  end_time    TIME NOT NULL
);

CREATE TABLE doctor_holidays (
                                 id           BIGSERIAL PRIMARY KEY,
                                 doctor_id    BIGINT NOT NULL REFERENCES app_users (id),
                                 holiday_date DATE NOT NULL
);

CREATE TABLE verification_token (
                                    id              BIGSERIAL PRIMARY KEY,
                                    token           VARCHAR(255) NOT NULL UNIQUE,
                                    expires_at      TIMESTAMP(6) NOT NULL,
                                    user_id         BIGINT NOT NULL UNIQUE REFERENCES app_users (id),
                                    used            BOOLEAN NOT NULL,
                                    failed_attempts INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE password_reset_tokens (
                                       id              BIGSERIAL PRIMARY KEY,
                                       token           VARCHAR(255) NOT NULL,
                                       expires_at      TIMESTAMP(6) NOT NULL,
                                       used            BOOLEAN NOT NULL,
                                       user_id         BIGINT NOT NULL REFERENCES app_users (id),
                                       failed_attempts INTEGER NOT NULL DEFAULT 0
);