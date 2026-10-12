package com.portofolio.medical_app.exception;

import com.portofolio.medical_app.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Unsere eigenen, bewusst geworfenen Fehler (Status kommt aus der Exception)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponseDto> handleAppException(AppException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    // Kaputtes JSON, falsches Datumsformat, fehlende Parameter, ungültige Uhrzeit
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            DateTimeParseException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception ex) {
        LOGGER.debug("Ungültige Anfrage: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Ungültige Anfrage. Bitte überprüfe deine Eingaben.");
    }

    // @PreAuthorize schlägt fehl (z.B. Patient ruft Arzt-Endpoint auf)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied() {
        return build(HttpStatus.FORBIDDEN, "Du hast keine Berechtigung für diese Aktion.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthentication() {
        return build(HttpStatus.UNAUTHORIZED, "Bitte melde dich an.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound() {
        return build(HttpStatus.NOT_FOUND, "Die angeforderte Ressource wurde nicht gefunden.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotAllowed() {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Diese HTTP-Methode wird nicht unterstützt.");
    }

    // Auffangnetz: Details nur ins Log, nie zum Client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception ex) {
        LOGGER.error("Unerwarteter Fehler", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Es ist ein unerwarteter Fehler aufgetreten.");
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Ungültige Eingabe.");
        return build(HttpStatus.BAD_REQUEST, message);
    }
}