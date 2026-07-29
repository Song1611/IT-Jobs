package com.itjob.constant;

import java.time.Duration;

public final class TrendingConstant {

    public static final long TTL_HOURS = 48;
    public static final double DECAY_FACTOR = 0.8;
    public static final Duration TRANSITION_LOCK_TTL = Duration.ofMinutes(5);

    private TrendingConstant() {}
}
