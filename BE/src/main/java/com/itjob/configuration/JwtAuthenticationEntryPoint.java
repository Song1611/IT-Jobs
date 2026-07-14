package com.itjob.configuration;

import com.itjob.exception.ErrorCode;
import com.itjob.util.SecurityResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authentication failures in Security Filter Chain
 * Triggered when:
 * - No JWT token provided
 * - Invalid JWT token
 * - Expired JWT token
 * Returns 401 UNAUTHORIZED with ApiResponse format
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        
        log.warn("Authentication failed for request to: {} - Reason: {}", 
                request.getRequestURI(), authException.getMessage());

        SecurityResponseUtil.writeErrorResponse(response, ErrorCode.UNAUTHENTICATED);
    }
}
