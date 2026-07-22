package com.itjob.service;

public interface RateLimitService {

    boolean tryAcquire(String name, String identifier, int limit, int durationSeconds);
}
