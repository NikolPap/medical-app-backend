package com.portofolio.medical_app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;

    // Standard: 400 Bad Request
    public AppException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // Für technische Fehler (z.B. Security-Konfiguration)
    public AppException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

}