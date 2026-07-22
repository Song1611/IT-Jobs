package com.itjob.annotation;

import com.itjob.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key();

    int limit();

    int duration();

    RateLimitType type() default RateLimitType.IP;
}
