package com.itjob.service;

import java.util.concurrent.TimeUnit;

public interface DistributedLockService {

    boolean tryLock(String key, String value, long leaseTime, TimeUnit unit);

    void unlock(String key, String value);
}
