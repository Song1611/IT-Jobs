package com.itjob.configuration;

import com.itjob.exception.ErrorCode;
import com.itjob.util.SecurityResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authorization failures in Security Filter Chain
 * Triggered when:
 * - User authenticated but lacks required role
 * - @PreAuthorize or @PostAuthorize fails
 * - Insufficient permissions for resource
 * Returns 403 FORBIDDEN with ApiResponse format
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    
    @Override
    public void handle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AccessDeniedException accessDeniedException) throws IOException {
        
        log.warn("Access denied for user at path: {} - Reason: {}", 
                request.getRequestURI(), accessDeniedException.getMessage());
        
        SecurityResponseUtil.writeErrorResponse(response, ErrorCode.FORBIDDEN);
    }
}
