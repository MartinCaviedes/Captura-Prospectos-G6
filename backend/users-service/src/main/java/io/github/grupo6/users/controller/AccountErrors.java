package io.github.grupo6.users.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountErrors {
    @ExceptionHandler(AccountException.class)
    ResponseEntity<Map<String,String>> business(AccountException error) {
        return ResponseEntity.status(error.status()).body(Map.of("code", error.code(), "message", error.getMessage()));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class})
    ResponseEntity<Map<String,String>> invalid(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message","Revisa los datos enviados."));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> duplicate(Exception error) {
        return ResponseEntity.status(409).body(Map.of("code","ACCOUNT_EXISTS","message","La cuenta ya existe o sus datos entran en conflicto."));
    }
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Map<String,String>> conflict(Exception error) {
        return ResponseEntity.status(409).body(Map.of("code","VERSION_CONFLICT","message","Actualiza los datos antes de guardar."));
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<Map<String,String>> unavailable(Exception error) {
        return ResponseEntity.status(503).body(Map.of("code","SERVICE_UNAVAILABLE","message","Servicio temporalmente no disponible."));
    }
}
