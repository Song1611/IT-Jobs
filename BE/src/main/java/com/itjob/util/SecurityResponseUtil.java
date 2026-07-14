package com.itjob.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itjob.dto.response.ApiResponse;
import com.itjob.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Utility class for writing security error responses
 * Used by JwtAuthenticationEntryPoint and CustomAccessDeniedHandler
 * to avoid code duplication
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityResponseUtil {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * Write error response as JSON to HttpServletResponse
     * 
     * @param response HttpServletResponse to write to
     * @param errorCode ErrorCode containing status code and message
     * @throws IOException if writing fails
     */
    public static void writeErrorResponse(
            HttpServletResponse response, 
            ErrorCode errorCode) throws IOException {
        
        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
