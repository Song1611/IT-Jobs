package com.itjob.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itjob.dto.response.ApiResponse;
import com.itjob.exception.ErrorCode;
import com.itjob.util.SecurityResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectProvider<List<HandlerMapping>> handlerMappingsProvider;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JwtAuthenticationEntryPoint(ObjectProvider<List<HandlerMapping>> handlerMappingsProvider) {
        this.handlerMappingsProvider = handlerMappingsProvider;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        log.warn("Authentication failed for request to: {} - Reason: {}",
                request.getRequestURI(), authException.getMessage());

        if (!isHandlerExists(request)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<?> body = ApiResponse.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("Not found")
                    .build();
            OBJECT_MAPPER.writeValue(response.getWriter(), body);
            return;
        }

        SecurityResponseUtil.writeErrorResponse(response, ErrorCode.UNAUTHENTICATED);
    }

    private boolean isHandlerExists(HttpServletRequest request) {
        List<HandlerMapping> mappings = handlerMappingsProvider.getIfAvailable();
        if (mappings == null) {
            return false;
        }
        try {
            for (HandlerMapping mapping : mappings) {
                if (mapping instanceof RequestMappingHandlerMapping) {
                    Object handler = mapping.getHandler(request);
                    if (handler != null) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error checking handler for path: {}", request.getRequestURI(), e);
        }
        return false;
    }
}
