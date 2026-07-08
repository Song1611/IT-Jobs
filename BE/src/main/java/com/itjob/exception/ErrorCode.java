package com.itjob.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // 1000-1999: Validation
    USERNAME_INVALID(1001, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1002, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1003, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),

    // 2000-2999: Authentication
    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    // 3000-3999: Authorization
    UNAUTHORIZED(3001, "You do not have permission", HttpStatus.FORBIDDEN),

    // 4000-4999: Business
    USER_EXISTED(4001, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(4002, "User not found", HttpStatus.NOT_FOUND),
    JOB_NOT_FOUND(4003, "Job not found", HttpStatus.NOT_FOUND),
    JOB_NOT_OPEN(4004, "Job is not open for applications", HttpStatus.BAD_REQUEST),
    COMPANY_NOT_FOUND(4005, "Company not found", HttpStatus.NOT_FOUND),
    APPLICATION_NOT_FOUND(4006, "Application not found", HttpStatus.NOT_FOUND),
    ALREADY_APPLIED(4007, "You have already applied for this job", HttpStatus.BAD_REQUEST),
    CANNOT_WITHDRAW_APPLICATION(4008, "Cannot withdraw this application", HttpStatus.BAD_REQUEST),

    // 9000-9999: System
    UNCATEGORIZED_EXCEPTION(9999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR
    ),
    ;
    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
