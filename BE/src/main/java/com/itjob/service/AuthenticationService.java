package com.itjob.service;

import com.itjob.dto.request.AuthenticationRequest;
import com.itjob.dto.request.LogoutRequest;
import com.itjob.dto.request.RefreshRequest;
import com.itjob.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);
    AuthenticationResponse refreshToken(RefreshRequest request);
    void logout(LogoutRequest request);
}
