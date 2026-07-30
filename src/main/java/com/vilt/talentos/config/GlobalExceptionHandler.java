package com.vilt.talentos.config;

import com.vilt.talentos.dto.ApiError;
import com.vilt.talentos.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        ApiError error = new ApiError(
            ex.getStatus().value(),
            ex.getMessage(),
            Instant.now()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        ApiError error = new ApiError(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            "O formato da requisição não é suportado. Por favor, utilize 'application/json'.",
            Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        String message = name.equals("email") ? "O e-mail é obrigatório." : "O parâmetro " + name + " é obrigatório.";
        
        ApiError error = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            message,
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        ApiError error = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            "Erro de validação: " + message,
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
        ApiError error = new ApiError(
            ex.getStatusCode().value(),
            ex.getReason(),
            Instant.now()
        );
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Violação de integridade de dados", ex);

        String message = "Não foi possível concluir a operação. Verifique se os dados informados já existem.";
        String details = ex.getMostSpecificCause().getMessage();
        if (details != null) {
            if (details.contains("cpf") || details.contains("uk_profiles_cpf")) {
                message = "CPF já cadastrado.";
            } else if (details.contains("email") || details.contains("users_email")) {
                message = "E-mail já em uso.";
            }
        }

        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), message, Instant.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex) {
        ApiError error = new ApiError(
            500,
            "Erro interno no servidor: " + ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.internalServerError().body(error);
    }
}
