package com.itjob.constant;

import java.time.Duration;

public final class OtpConstant {

    private OtpConstant() {
    }

    public static final int OTP_LENGTH = 6;
    public static final int OTP_MIN = (int) Math.pow(10, OTP_LENGTH - 1);
    public static final int OTP_MAX = (int) Math.pow(10, OTP_LENGTH) - 1;
    public static final Duration OTP_TTL = Duration.ofMinutes(5);
    public static final Duration ATTEMPT_TTL = Duration.ofMinutes(5);
    public static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    public static final Duration SEND_LIMIT_TTL = Duration.ofHours(1);
    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_SENDS = 5;
}
