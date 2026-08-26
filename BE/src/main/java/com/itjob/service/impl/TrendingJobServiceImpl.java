package com.itjob.service.impl;

import com.itjob.annotation.DistributedLock;
import com.itjob.constant.TrendingConstant;
import com.itjob.redis.RedisKeys;
import com.itjob.service.TrendingJobService;
import com.itjob.util.RedisOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        RedisOperation.run(() -> {
            String key = RedisKeys.trendingDailyKey();
            String member = jobId.toString();
            Boolean exists = stringRedisTemplate.hasKey(key);
            stringRedisTemplate.opsForZSet().incrementScore(key, member, score);
            if (!exists) {
                stringRedisTemplate.expire(key, TrendingConstant.TTL_HOURS, TimeUnit.HOURS);
            }
        }, "Failed to record trending score for job {}", jobId);
    }

    @Override
    public List<UUID> getTopJobIds(int limit) {
        Set<String> result = RedisOperation.supply(() -> {
            var typedOps = stringRedisTemplate.opsForZSet();
            return typedOps.reverseRange(RedisKeys.trendingDailyKey(), 0, (long)limit - 1);
        }, "Failed to get trending jobs");

        return RedisOperation.parseUuids(result);
    }

    @Override
    public void removeJob(UUID jobId) {
        RedisOperation.run(() -> {
            String member = jobId.toString();
            stringRedisTemplate.opsForZSet().remove(RedisKeys.trendingDailyKey(), member);
            stringRedisTemplate.opsForZSet().remove(RedisKeys.trendingDailyKey(LocalDate.now().minusDays(1)), member);
        }, "Failed to remove job {} from trending", jobId);
    }

    @Scheduled(cron = "0 5 0 * * ?")
    @DistributedLock(key = "'trending-transition'", leaseTime = 120)
    public void transitionDaily() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdayKey = RedisKeys.trendingDailyKey(yesterday);
        String todayKey = RedisKeys.trendingDailyKey();

        RedisOperation.run(() -> {
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
        }, "Failed to apply daily trending decay");
    }
}
