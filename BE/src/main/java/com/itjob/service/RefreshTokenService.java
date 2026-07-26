package com.itjob.service;

import com.itjob.entity.RefreshToken;
import com.itjob.entity.User;

public interface RefreshTokenService {

    String createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    RefreshToken revokeRefreshToken(String token);

    void revokeAllUserTokens(User user);
}
