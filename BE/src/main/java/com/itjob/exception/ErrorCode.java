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
    REFRESH_TOKEN_INVALID(2002, "Refresh token is missing or invalid", HttpStatus.UNAUTHORIZED),

    // 3000-3999: Authorization
    FORBIDDEN(3001, "You do not have permission", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(3002, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),

    // 4000-4999: Business
    USER_EXISTED(4001, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(4002, "User not found", HttpStatus.NOT_FOUND),
    JOB_NOT_FOUND(4003, "Job not found", HttpStatus.NOT_FOUND),
    JOB_NOT_OPEN(4004, "Job is not open for applications", HttpStatus.BAD_REQUEST),
    COMPANY_NOT_FOUND(4005, "Company not found", HttpStatus.NOT_FOUND),
    APPLICATION_NOT_FOUND(4006, "Application not found", HttpStatus.NOT_FOUND),
    ALREADY_APPLIED(4007, "You have already applied for this job", HttpStatus.BAD_REQUEST),
    CANNOT_WITHDRAW_APPLICATION(4008, "Cannot withdraw this application", HttpStatus.BAD_REQUEST),
    BLOG_NOT_FOUND(4009, "Blog not found", HttpStatus.NOT_FOUND),
    BLOG_CATEGORY_NOT_FOUND(4010, "Blog category not found", HttpStatus.NOT_FOUND),
    COMPANY_ALREADY_EXISTS(4011, "You already have a company", HttpStatus.BAD_REQUEST),
    COMPANY_NOT_ACTIVE(4012, "Company is not active", HttpStatus.BAD_REQUEST),
    SKILL_NOT_FOUND(4013, "One or more skills not found", HttpStatus.NOT_FOUND),
    INVALID_LIMIT(4014, "Limit must be greater than 0", HttpStatus.BAD_REQUEST),
    LIMIT_EXCEEDED(4015, "Limit cannot exceed 100", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_FOUND(4016, "Review not found", HttpStatus.NOT_FOUND),
    ALREADY_REVIEWED(4017, "You have already reviewed this company", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS(4290, "Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    RESOURCE_BUSY(4019, "Resource is currently being processed. Please try again", HttpStatus.LOCKED),
    COMPANY_ALREADY_PROCESSED(4020, "Company has already been processed", HttpStatus.BAD_REQUEST),
    JOB_ALREADY_PROCESSED(4021, "Job has already been processed", HttpStatus.BAD_REQUEST),
    INVALID_KEY(4018, "Invalid lock key expression", HttpStatus.INTERNAL_SERVER_ERROR),

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
