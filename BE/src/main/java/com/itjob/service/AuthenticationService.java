package com.itjob.service;

import com.itjob.dto.request.*;
import com.itjob.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    AuthenticationResponse refreshToken(RefreshRequest request);
    void logout(LogoutRequest request);
    void register(RegisterRequest request);
    void verifyEmail(VerifyEmailRequest request);
    void resendOtp(ResendOtpRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
