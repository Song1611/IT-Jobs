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

    // ========== OTP ==========
    public static final String OTP_PREFIX = "otp";

    // ========== LOCK ==========
    public static final String LOCK_PREFIX = "lock";

    // ========== RATE LIMIT ==========
    public static final String RATE_LIMIT_PREFIX = "ratelimit";

    public static String rateLimitKey(String name, String identifier) {
        return RATE_LIMIT_PREFIX + ":" + name + ":" + identifier;
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
