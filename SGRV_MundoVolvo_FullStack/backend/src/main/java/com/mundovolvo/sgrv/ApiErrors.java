package com.mundovolvo.sgrv;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ApiException.class)
 public ResponseEntity<?> known(ApiException e) { return ResponseEntity.status(e.status).body(Map.of("error", e.getMessage())); }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
  return ResponseEntity.badRequest().body(Map.of("error",e.getBindingResult().getFieldErrors().stream().findFirst().map(f->f.getField()+": "+f.getDefaultMessage()).orElse("Datos inválidos")));
 }
 @ExceptionHandler(HttpMessageNotReadableException.class)
 public ResponseEntity<?> unreadable() { return ResponseEntity.badRequest().body(Map.of("error","Solicitud JSON inválida")); }
 @ExceptionHandler(DataIntegrityViolationException.class)
 public ResponseEntity<?> integrity() { return ResponseEntity.status(409).body(Map.of("error","Conflicto de integridad: el registro ya existe o tiene operaciones asociadas")); }
 @ExceptionHandler(AccessDeniedException.class)
 public ResponseEntity<?> denied() { return ResponseEntity.status(403).body(Map.of("error","Permiso insuficiente")); }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<?> unexpected(Exception e) { return ResponseEntity.status(500).body(Map.of("error","Error interno. Revise los registros del servidor.")); }
}
