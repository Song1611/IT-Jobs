package com.itjob.service.impl;

import com.itjob.annotation.DistributedLock;
import com.itjob.constant.TrendingConstant;
import com.itjob.redis.RedisKeys;
import com.itjob.service.TrendingJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingJobServiceImpl implements TrendingJobService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void recordScore(UUID jobId, double score) {
        try {
            String key = RedisKeys.trendingDailyKey();
            stringRedisTemplate.opsForZSet().incrementScore(key, jobId.toString(), score);
            stringRedisTemplate.expire(key, TrendingConstant.TTL_HOURS, TimeUnit.HOURS);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, skipping trending score for job {}: {}", jobId, e.getMessage());
        } catch (DataAccessException e) {
            log.warn("Failed to record trending score for job {}: {}", jobId, e.getMessage());
        }
    }

    @Override
    public List<UUID> getTopJobIds(int limit) {
        try {
            var typedOps = stringRedisTemplate.opsForZSet();
            Set<String> result = typedOps.reverseRange(RedisKeys.trendingDailyKey(), 0, limit - 1);
            if (result == null || result.isEmpty()) {
                return Collections.emptyList();
            }
            return result.stream()
                    .map(id -> { try { return UUID.fromString(id); } catch (IllegalArgumentException e) { return null; } })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, cannot get trending jobs: {}", e.getMessage());
            return Collections.emptyList();
        } catch (DataAccessException e) {
            log.warn("Failed to get trending jobs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void removeJob(UUID jobId) {
        try {
            stringRedisTemplate.opsForZSet().remove(RedisKeys.trendingDailyKey(), jobId.toString());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, skipping removal of job {} from trending: {}", jobId, e.getMessage());
        } catch (DataAccessException e) {
            log.warn("Failed to remove job {} from trending: {}", jobId, e.getMessage());
        }
    }

    @Scheduled(cron = "0 5 0 * * ?")
    @DistributedLock(key = "'trending-transition'", leaseTime = 120)
    public void transitionDaily() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdayKey = RedisKeys.trendingDailyKey(yesterday);
        String todayKey = RedisKeys.trendingDailyKey();

        try {
            Boolean exists = stringRedisTemplate.hasKey(yesterdayKey);
            if (!exists) {
                log.debug("No yesterday trending data to decay");
                return;
            }

            var ops = stringRedisTemplate.opsForZSet();
            Set<ZSetOperations.TypedTuple<String>> tuples = ops.reverseRangeWithScores(yesterdayKey, 0, -1);
            if (tuples == null || tuples.isEmpty()) {
                return;
            }

            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                Double score = tuple.getScore();
                String member = tuple.getValue();
                if (member != null && score != null && score > 0) {
                    ops.incrementScore(todayKey, member, score * TrendingConstant.DECAY_FACTOR);
                }
            }

            stringRedisTemplate.expire(todayKey, TrendingConstant.TTL_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.delete(yesterdayKey);
            log.info("Decayed {} entries from {} into {} (factor={})",
                    tuples.size(), yesterdayKey, todayKey, TrendingConstant.DECAY_FACTOR);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable for daily trending decay: {}", e.getMessage());
        } catch (DataAccessException e) {
            log.warn("Failed to apply daily trending decay: {}", e.getMessage());
        }
    }
}
