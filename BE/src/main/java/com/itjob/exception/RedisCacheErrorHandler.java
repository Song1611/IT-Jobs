package com.itjob.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.lang.Nullable;

@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(
            RuntimeException exception,
            Cache cache,
            Object key) {

        log.error("Redis GET error. Cache={}, Key={}",
                cache.getName(),
                key,
                exception);

        // Evict the corrupt entry so the next read falls back to DB
        // and re-caches fresh data with the current serializer
        try {
            cache.evict(key);
            log.warn("Evicted corrupt cache entry. Cache={}, Key={}",
                    cache.getName(), key);
        } catch (Exception evictException) {
            log.error("Failed to evict corrupt cache entry. Cache={}, Key={}",
                    cache.getName(), key, evictException);
        }
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception,
            Cache cache,
            Object key,
            Object value) {

        log.error("Redis PUT error. Cache={}, Key={}",
                cache.getName(),
                key,
                exception);
    }

    @Override
    public void handleCacheEvictError(
            RuntimeException exception,
            Cache cache,
            Object key) {

        log.error("Redis EVICT error. Cache={}, Key={}",
                cache.getName(),
                key,
                exception);
    }

    @Override
    public void handleCacheClearError(
            RuntimeException exception,
            Cache cache) {

        log.error("Redis CLEAR error. Cache={}",
                cache.getName(),
                exception);
    }
}
