package com.itjob.service.impl;

import com.itjob.redis.RedisKeys;
import com.itjob.service.RefreshTokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenBlacklistServiceImpl implements RefreshTokenBlacklistService {

    private static final java.util.HexFormat HEX = java.util.HexFormat.of();

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String token, Instant expiredAt) {

        Duration ttl = Duration.between(Instant.now(), expiredAt);

        if (ttl.isZero() || ttl.isNegative()) {
            log.debug("Token already expired, skipping blacklist");
            return;
        }

        redisTemplate.opsForValue().set(
                RedisKeys.refreshBlacklist(hashToken(token)),
                "1",
                ttl
        );

        log.debug("Refresh token blacklisted with TTL {}s", ttl.toSeconds());
    }

    @Override
    public boolean isBlacklisted(String token) {

        return redisTemplate.hasKey(RedisKeys.refreshBlacklist(hashToken(token)));
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
