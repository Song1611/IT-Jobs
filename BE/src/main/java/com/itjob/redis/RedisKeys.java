package com.itjob.redis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class RedisKeys {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private RedisKeys() {
    }

    // ========== VIEW COUNTER ==========
    public static final String VIEW_PREFIX = "views";
    public static final String DIRTY_VIEW_SET = "dirty:views";
    public static final String VIEWED_PREFIX = "viewed";

    // ========== TRENDING JOBS ==========
    private static final String TRENDING_PREFIX = "trending";

    public static String trendingDailyKey() {
        return TRENDING_PREFIX + ":" + LocalDate.now().format(DATE_FMT);
    }

    public static String trendingDailyKey(LocalDate date) {
        return TRENDING_PREFIX + ":" + date.format(DATE_FMT);
    }

    // ========== REACTION COUNTER ==========
    public static final String REACTION_PREFIX = "reactions";
    public static final String DIRTY_REACTION_SET = "dirty:reactions";

    public static String reactionKey(String entity, Object id) {
        return REACTION_PREFIX + ":" + entity + ":" + id;
    }

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

    // ========== RECENTLY VIEWED ==========
    private static final String RECENT_VIEW_PREFIX = "recent";

    public static String recentViewKey(UUID userId) {
        return RECENT_VIEW_PREFIX + ":user:" + userId;
    }

    // ========== RECOMMENDATIONS ==========
    private static final String RECOMMEND_PREFIX = "recommend";

    public static String recommendKey(UUID userId) {
        return RECOMMEND_PREFIX + ":user:" + userId;
    }

    // ========== SEARCH HISTORY ==========
    private static final String SEARCH_HISTORY_PREFIX = "search:history";

    public static String searchHistoryKey(UUID userId) {
        return SEARCH_HISTORY_PREFIX + ":user:" + userId;
    }

    // ========== SEARCH SUGGESTION ==========
    private static final String SUGGEST_PREFIX = "suggest";

    public static String suggestPrefixKey(String prefix) {
        return SUGGEST_PREFIX + ":" + prefix;
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
