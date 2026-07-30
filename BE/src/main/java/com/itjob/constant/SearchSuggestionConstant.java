package com.itjob.constant;

import java.time.Duration;

public final class SearchSuggestionConstant {

    public static final String REDIS_KEYWORD_ZSET = "keyword:score";
    public static final String REDIS_SUGGEST_PREFIX = "suggest";
    public static final int MAX_RESULTS = 10;
    public static final int MAX_KEYWORD_LENGTH = 100;
    public static final int MAX_PREFIX_LENGTH = 20;
    public static final Duration SUGGEST_TTL = Duration.ofDays(30);

    private SearchSuggestionConstant() {}
}
