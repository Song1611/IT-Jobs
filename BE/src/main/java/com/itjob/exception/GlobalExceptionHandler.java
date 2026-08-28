package com.itjob.exception;

import com.itjob.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler { 

    /**
     * Handle business logic exceptions (AppException)
     * These are exceptions thrown intentionally in the service layer
     */
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        
        log.warn("AppException: code={}, message={}", 
                errorCode.getCode(), errorCode.getMessage());
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    /**
     * Handle AccessDeniedException from Service layer
     * Note: This only catches exceptions thrown in Controller/Service,
     * NOT from Security Filter Chain (those are handled by CustomAccessDeniedHandler)
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        
        log.warn("AccessDeniedException in service layer: {}", exception.getMessage());
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    /**
     * Handle UsernameNotFoundException
     * Thrown when user lookup fails during authentication
     */
    @ExceptionHandler(value = UsernameNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handlingUsernameNotFoundException(UsernameNotFoundException exception) {
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
        
        log.warn("UsernameNotFoundException: {}", exception.getMessage());
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    /**
     * Handle all uncaught exceptions
     * This is the fallback handler for unexpected errors
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handlingValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("VALIDATION_FAILED");

        log.warn("Validation failed: {}", message);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiResponse);
    }

    /**
     * Handle constraint violations from @Validated parameters (e.g. @Size on path/query params)
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handlingConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("VALIDATION_FAILED");

        log.warn("Constraint violation: {}", message);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiResponse);
    }

    /**
     * Handle all uncaught exceptions
     * This is the fallback handler for unexpected errors
     */
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingUncategorizedException(Exception exception) {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        
        log.error("Uncategorized exception: ", exception);
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }
}
