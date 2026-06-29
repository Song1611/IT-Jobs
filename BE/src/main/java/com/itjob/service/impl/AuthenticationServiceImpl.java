package com.itjob.service.impl;

import com.itjob.dto.request.AuthenticationRequest;
import com.itjob.dto.request.LogoutRequest;
import com.itjob.dto.request.RefreshRequest;
import com.itjob.dto.response.AuthenticationResponse;
import com.itjob.entity.RefreshToken;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.UserRepository;
import com.itjob.service.AuthenticationService;
import com.itjob.service.JwtService;
import com.itjob.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.debug("Authenticating user: {}", request.getUsername());
        
        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Authentication not successful: {}", request.getUsername());
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matched) {
            log.warn("Invalid password for user: {}", request.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Note: NOT revoking old tokens here to support multiple device login
        // If you want single device login only, uncomment this:
        // refreshTokenService.revokeAllUserTokens(user);
        
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.debug("User authenticated successfully: {}", request.getUsername());
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) {
        log.debug("Refreshing access token");
        
        // 1. Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        
        // 2. Get user by username from refresh token
        User user = userRepository.findByEmail(refreshToken.getUsername())
                .orElseThrow(() -> {
                    log.warn("Authentication failed for user: {}", refreshToken.getUsername());
                    return new AppException(ErrorCode.UNAUTHENTICATED);

                });

        // 3. Revoke old refresh token FIRST (important for rotation)
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        
        // 4. Generate new access token
        String accessToken = jwtService.generateAccessToken(user);
        
        // 5. Create new refresh token
        String newRefreshToken = refreshTokenService.createRefreshToken(user);
        
        log.debug("Access token refreshed successfully for user: {}", user.getEmail());
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .authenticated(true)
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.debug("Logging out user");
        
        // Revoke the refresh token
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        
        log.debug("User logged out successfully");
    }
}
