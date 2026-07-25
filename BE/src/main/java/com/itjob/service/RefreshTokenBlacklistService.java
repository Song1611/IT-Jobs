package com.itjob.service;

import java.time.Instant;

public interface RefreshTokenBlacklistService {

    void blacklist(String refreshToken, Instant expiredAt);

    boolean isBlacklisted(String refreshToken);
}
