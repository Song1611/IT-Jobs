package com.itjob.exception;

import com.itjob.dto.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handlingAppException -> maps AppException to ApiResponse with error code and status")
    void appExceptionMapsToApiResponse() {
        // Act
        ResponseEntity<ApiResponse<Void>> response =
                handler.handlingAppException(new AppException(ErrorCode.USER_NOT_FOUND));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiResponse<Void> body = response.getBody();
        assertThat(body.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
        assertThat(body.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        assertThat(body.getResult()).isNull();
    }

    @Test
    @DisplayName("handlingAccessDeniedException -> returns ACCESS_DENIED with forbidden status")
    void accessDeniedMapsToApiResponse() {
        // Act
        ResponseEntity<ApiResponse<Void>> response =
                handler.handlingAccessDeniedException(new AccessDeniedException("denied"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.ACCESS_DENIED.getStatusCode());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.ACCESS_DENIED.getCode());
    }

    @Test
    @DisplayName("handlingUncategorizedException -> returns UNCATEGORIZED_EXCEPTION for unexpected errors")
    void uncategorizedExceptionMapsToApiResponse() {
        // Act
        ResponseEntity<ApiResponse<Void>> response =
                handler.handlingUncategorizedException(new IllegalStateException("boom"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
    }
}