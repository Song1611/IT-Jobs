package com.itjob.service;

import com.itjob.entity.User;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface JwtService {

    String generateAccessToken(User user);
    
    /**
     * Extract user ID from JWT authentication.
     * 
     * @param authentication the authentication object containing JWT
     * @return the user ID from JWT claims
     * @throws IllegalArgumentException if authentication is null or invalid
     * @throws IllegalStateException if userId claim is missing or invalid
     */
    UUID extractUserId(Authentication authentication);
    
    /**
     * Safely extract user ID from JWT authentication, returning null if authentication is null.
     * 
     * @param authentication the authentication object containing JWT (can be null)
     * @return the user ID from JWT claims, or null if authentication is null
     * @throws IllegalArgumentException if authentication is not null but invalid
     * @throws IllegalStateException if userId claim is missing or invalid
     */
    UUID extractUserIdSafely(Authentication authentication);
}
