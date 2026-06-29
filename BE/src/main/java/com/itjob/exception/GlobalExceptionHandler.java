package com.itjob.exception;

import com.itjob.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler { 

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingException (RuntimeException exception) {
        log.error("Exception :", exception);
        ApiResponse<?> apiResponse =  ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();

        return ResponseEntity.status(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode())
                .body(apiResponse);

    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException (AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<?> apiResponse = ApiResponse.builder().code(errorCode.getCode())
                .message(errorCode.getMessage()).build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<?>> handlingAccessDeniedException (AccessDeniedException exception) {
        ApiResponse<?> apiResponse = ApiResponse.builder().code(ErrorCode.UNAUTHORIZED.getCode())
                .message(ErrorCode.UNAUTHORIZED.getMessage()).build();
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = UsernameNotFoundException.class)
    ResponseEntity<ApiResponse<?>> handlingUsernameNotFoundException (UsernameNotFoundException exception) {
        ApiResponse<?> apiResponse = ApiResponse.builder().code(ErrorCode.USER_NOT_FOUND.getCode())
                .message(ErrorCode.USER_NOT_FOUND.getMessage()).build();
        return ResponseEntity.status(ErrorCode.USER_NOT_FOUND.getStatusCode()).body(apiResponse);
    }
}
