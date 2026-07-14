package com.itjob.exception;

import com.itjob.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    ResponseEntity<ApiResponse<?>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        
        log.warn("AppException: code={}, message={}", 
                errorCode.getCode(), errorCode.getMessage());
        
        ApiResponse<?> apiResponse = ApiResponse.builder()
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
    ResponseEntity<ApiResponse<?>> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        
        log.warn("AccessDeniedException in service layer: {}", exception.getMessage());
        
        ApiResponse<?> apiResponse = ApiResponse.builder()
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
    ResponseEntity<ApiResponse<?>> handlingUsernameNotFoundException(UsernameNotFoundException exception) {
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
        
        log.warn("UsernameNotFoundException: {}", exception.getMessage());
        
        ApiResponse<?> apiResponse = ApiResponse.builder()
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
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingUncategorizedException(Exception exception) {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        
        log.error("Uncategorized exception: ", exception);
        
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }
}
