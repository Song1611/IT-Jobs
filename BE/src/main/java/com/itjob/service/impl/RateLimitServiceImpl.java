package com.itjob.service.impl;

import com.itjob.redis.RedisKeys;
import com.itjob.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();

    static {
        RATE_LIMIT_SCRIPT.setScriptText(
                "local key = KEYS[1] " +
                "local limit = tonumber(ARGV[1]) " +
                "local ttl = tonumber(ARGV[2]) " +
                "local count = redis.call('INCR', key) " +
                "if count == 1 then " +
                "  redis.call('EXPIRE', key, ttl) " +
                "end " +
                "return count"
        );
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryAcquire(String name, String identifier, int limit, int durationSeconds) {
        String key = RedisKeys.rateLimitKey(name, identifier);
        try {
            Long count = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(limit),
                    String.valueOf(durationSeconds)
            );
            if (count > limit) {
                log.warn("Rate limit exceeded for {}:{} (count={}, limit={})", name, identifier, count, limit);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return true;
        }
    }
}
