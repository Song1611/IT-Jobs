package com.itjob.service.impl;

import com.itjob.constant.RecentViewConstant;
import com.itjob.redis.RedisKeys;
import com.itjob.service.RecentViewService;
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
public class RecentViewServiceImpl implements RecentViewService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void recordView(UUID userId, UUID jobId) {
        if (userId == null || jobId == null) {
            return;
        }
        RedisOperation.run(() -> {
            String key = RedisKeys.recentViewKey(userId);
            String member = jobId.toString();
            stringRedisTemplate.opsForList().remove(key, 1, member);
            stringRedisTemplate.opsForList().leftPush(key, member);
            stringRedisTemplate.opsForList().trim(key, 0, (long) RecentViewConstant.MAX_SIZE - 1);
            stringRedisTemplate.expire(key, RecentViewConstant.TTL_DAYS, TimeUnit.DAYS);
        }, "Failed to record recent view for user {}: {}", userId);
    }

    @Override
    public List<UUID> getRecentViewIds(UUID userId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<String> ids = RedisOperation.supply(() -> {
            String key = RedisKeys.recentViewKey(userId);
            return stringRedisTemplate.opsForList().range(key, 0, (long)limit - 1);
        }, "Failed to get recent views for user {}: {}", userId);

        return RedisOperation.parseUuids(ids);
    }
}
