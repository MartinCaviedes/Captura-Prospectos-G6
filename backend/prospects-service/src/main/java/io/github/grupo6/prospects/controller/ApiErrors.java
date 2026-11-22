package io.github.grupo6.prospects.controller;

import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    public record Error(String code, String message, Map<String,String> fieldErrors, String correlationId) {}
    public static Error error(String code, String message) { return new Error(code,message,Map.of(),UUID.randomUUID().toString()); }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Error> status(ResponseStatusException ex) {
        String message = switch(ex.getStatusCode().value()) {
            case 403 -> "No tiene permiso para esta operación";
            case 404 -> "Prospecto no encontrado";
            case 409 -> "El estado actual no permite la operación";
            default -> "Revisar los datos enviados";
        };
        return ResponseEntity.status(ex.getStatusCode()).body(error(ex.getReason(),message));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Error> validation(MethodArgumentNotValidException ex) {
        var fields = new java.util.TreeMap<String,String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(),"INVALID_VALUE"));
        return ResponseEntity.badRequest().body(new Error("INVALID_REQUEST","Revisar los campos",fields,UUID.randomUUID().toString()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Error> malformed(Exception ex) {
        return ResponseEntity.badRequest().body(error("INVALID_REQUEST","Formato de petición inválido"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Error> duplicate(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(error("DUPLICATE_EVENT_REGISTRATION","Ya existe una inscripción activa para este evento"));
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Error> conflict(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(409).body(error("CONCURRENT_UPDATE","El registro cambió; vuelva a consultarlo"));
    }
}
