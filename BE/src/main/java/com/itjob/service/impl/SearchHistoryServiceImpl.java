package com.itjob.service.impl;

import com.itjob.constant.SearchHistoryConstant;
import com.itjob.redis.RedisKeys;
import com.itjob.service.SearchHistoryService;
import com.itjob.util.RedisOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void recordSearch(UUID userId, String keyword) {
        if (userId == null || keyword == null || keyword.isBlank()) return;

        keyword = keyword.trim().replaceAll("\\s+", " ");
        if (keyword.length() > SearchHistoryConstant.MAX_LENGTH) {
            keyword = keyword.substring(0, SearchHistoryConstant.MAX_LENGTH);
        }

        String finalKeyword = keyword;
        RedisOperation.run(() -> {
            String key = RedisKeys.searchHistoryKey(userId);
            stringRedisTemplate.opsForList().remove(key, 1, finalKeyword);
            stringRedisTemplate.opsForList().leftPush(key, finalKeyword);
            stringRedisTemplate.opsForList().trim(key, 0, (long)SearchHistoryConstant.MAX_SIZE - 1);
            stringRedisTemplate.expire(key, SearchHistoryConstant.TTL_DAYS, TimeUnit.DAYS);
        }, "Failed to record search history for user {}: {}", userId, keyword);
    }

    @Override
    public List<String> getSearchHistory(UUID userId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        return RedisOperation.supply(() -> {
            String key = RedisKeys.searchHistoryKey(userId);
            return stringRedisTemplate.opsForList().range(key, 0, (long)limit - 1);
        }, "Failed to get search history for user {}", userId);
    }
}
