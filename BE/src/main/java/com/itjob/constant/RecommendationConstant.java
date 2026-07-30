package com.itjob.constant;

import java.time.Duration;

public final class RecommendationConstant {

    public static final int SCORE_SKILL_MATCH = 40;
    public static final int SCORE_LOCATION_MATCH = 20;
    public static final int SCORE_APPLIED_SAME_TYPE = 30;
    public static final int SCORE_RECENTLY_VIEWED = 5;
    public static final int SCORE_TRENDING = 5;
    public static final int CACHE_MAX_RESULTS = 20;
    public static final Duration CACHE_TTL = Duration.ofMinutes(30);
    public static final int MAX_CANDIDATES = 500;
    public static final int RECENT_JOB_LIMIT = 50;
    public static final int TRENDING_JOB_LIMIT = 50;

    private RecommendationConstant() {}
}
