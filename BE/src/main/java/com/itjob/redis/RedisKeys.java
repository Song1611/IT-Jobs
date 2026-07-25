package com.itjob.redis;

import java.time.Duration;

public final class RedisKeys {

    private RedisKeys() {
    }

    // ========== VIEW COUNTER ==========
    public static final String VIEW_PREFIX = "views";
    public static final String DIRTY_VIEW_SET = "dirty:views";
    public static final String VIEWED_PREFIX = "viewed";

    // ========== BLACKLIST ==========
    public static final String BLACKLIST_PREFIX = "blacklist";

    public static String refreshBlacklist(String token) {
        return BLACKLIST_PREFIX + ":refresh:" + token;
    }

    // ========== OTP ==========
    public static final String OTP_PREFIX = "otp";
    public static final String OTP_ATTEMPT_PREFIX = "otp_attempt";

    public static String otp(String email) {
        return OTP_PREFIX + ":" + email;
    }

    public static String otpAttempt(String email) {
        return OTP_ATTEMPT_PREFIX + ":" + email;
    }

    public static final String OTP_COOLDOWN_PREFIX = "otp_cooldown";
    public static final String OTP_SEND_LIMIT_PREFIX = "otp_send";

    public static String otpCooldown(String email) {
        return OTP_COOLDOWN_PREFIX + ":" + email;
    }

    public static String otpSendLimit(String email) {
        return OTP_SEND_LIMIT_PREFIX + ":" + email;
    }

    // ========== LOCK ==========
    public static final String LOCK_PREFIX = "lock";

    // ========== RATE LIMIT ==========
    public static final String RATE_LIMIT_PREFIX = "ratelimit";

    public static String rateLimitKey(String name, String identifier) {
        return RATE_LIMIT_PREFIX + ":" + name + ":" + identifier;
    }

    public static String lockKey(String name, Object... parts) {
        StringBuilder key = new StringBuilder(LOCK_PREFIX).append(":").append(name);
        for (Object part : parts) {
            key.append(":").append(part);
        }
        return key.toString();
    }

    /**
     * Build a view counter key: {@code views:{entity}:{id}}
     */
    public static String viewKey(String entity, Object id) {
        return VIEW_PREFIX + ":" + entity + ":" + id;
    }

    /**
     * Build a view debounce key: {@code viewed:{entity}:{id}:{viewerId}}
     * Prevents the same viewer from incrementing the same entity within the cooldown window.
     */
    public static String viewedKey(String entity, Object id, String viewerId) {
        return VIEWED_PREFIX + ":" + entity + ":" + id + ":" + viewerId;
    }

    /**
     * Returns the cooldown TTL for view debounce.
     */
    public static Duration viewDebounceTtl() {
        return CacheTTL.VIEW_DEBOUNCE.getTtl();
    }
}
