package com.votify.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Devuelve errores en JSON con un campo {@code message} para que el frontend pueda mostrarlos.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();
        String msg = cause != null && cause.getMessage() != null ? cause.getMessage() : e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "Cuerpo JSON inválido o campos con tipo incorrecto";
        }
        log.debug("JSON no legible: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        String msg = e.getMessage();
        if (e instanceof NullPointerException || msg == null || msg.isBlank()) {
            log.error("Error no controlado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error interno del servidor"));
        }
        if (msg.toLowerCase().contains("not found")) {
            log.debug("Recurso no encontrado: {}", msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", msg));
        }
        log.debug("Regla de negocio o validación: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }
}
