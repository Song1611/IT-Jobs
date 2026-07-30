package com.itjob.service.impl;

import com.itjob.constant.TrendingScore;
import com.itjob.redis.CacheTTL;
import com.itjob.redis.RedisKeys;
import com.itjob.enums.ViewEntity;
import com.itjob.repository.BlogRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.service.TrendingJobService;
import com.itjob.service.ViewCountService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountServiceImpl implements ViewCountService {

    private final RedisTemplate<String, Long> longRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final BlogRepository blogRepository;
    private final TrendingJobService trendingJobService;

    private Map<ViewEntity, BiFunction<UUID, Long, Integer>> dbUpdaters;

    @PostConstruct
    void initUpdaters() {
        dbUpdaters = Map.of(
                ViewEntity.JOB, jobRepository::incrementViewCount,
                ViewEntity.COMPANY, companyRepository::incrementViewCount,
                ViewEntity.BLOG, blogRepository::incrementViewCount
        );
    }

    @Override
    public void incrementView(ViewEntity entity, UUID id) {
        incrementView(entity, id, null);
    }

    @Override
    public void incrementView(ViewEntity entity, UUID id, String viewerId) {
        String redisKey = RedisKeys.viewKey(entity.getKey(), id);
        try {
            // Spam debounce: skip if same viewer already viewed within cooldown
            if (viewerId != null && !viewerId.isBlank()) {
                String viewedKey = RedisKeys.viewedKey(entity.getKey(), id, viewerId);
                Boolean alreadyViewed = stringRedisTemplate.opsForValue().setIfAbsent(viewedKey, "1", RedisKeys.viewDebounceTtl());
                if (Boolean.FALSE.equals(alreadyViewed)) {
                    return;
                }
            }

            Boolean first = longRedisTemplate.opsForValue().setIfAbsent(redisKey, 0L, CacheTTL.VIEW_KEY.getTtl());
            longRedisTemplate.opsForValue().increment(redisKey);
            if (Boolean.TRUE.equals(first)) {
                stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_VIEW_SET, redisKey);
            }

            if (entity == ViewEntity.JOB) {
                trendingJobService.recordScore(id, TrendingScore.VIEW);
            }
        } catch (Exception e) {
            log.warn("Failed to increment view for {}/{}: {}", entity.getKey(), id, e.getMessage());
        }
    }

    @Override
    public long getPendingViewDelta(ViewEntity entity, UUID id) {
        try {
            Long value = longRedisTemplate.opsForValue().get(RedisKeys.viewKey(entity.getKey(), id));
            return value == null ? 0L : value;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Scheduled(fixedDelayString = "${view.sync.interval:300000}")
    public void syncToDatabase() {
        Set<String> dirtyKeys;
        try {
            dirtyKeys = stringRedisTemplate.opsForSet().members(RedisKeys.DIRTY_VIEW_SET);
            if (dirtyKeys == null || dirtyKeys.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            log.error("Failed to read dirty view set: {}", e.getMessage());
            return;
        }

        for (String redisKey : dirtyKeys) {
            syncKey(redisKey);
        }
    }

    private void syncKey(String redisKey) {
        Long views = longRedisTemplate.opsForValue().getAndSet(redisKey, 0L);
        if (views == null || views <= 0L) {
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_VIEW_SET, redisKey);
            return;
        }

        UUID id = parseIdFromKey(redisKey);
        ViewEntity entity = extractEntity(redisKey);
        if (id == null || entity == null) {
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_VIEW_SET, redisKey);
            longRedisTemplate.unlink(redisKey);
            return;
        }

        BiFunction<UUID, Long, Integer> updater = dbUpdaters.get(entity);
        if (updater == null) {
            log.warn("No updater for entity: {}", entity);
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_VIEW_SET, redisKey);
            longRedisTemplate.unlink(redisKey);
            return;
        }

        try {
            int affected = updater.apply(id, views);
            if (affected == 0) {
                log.warn("No {} found for id {}, deleting stale key", entity.getKey(), id);
                longRedisTemplate.unlink(redisKey);
                stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_VIEW_SET, redisKey);
                return;
            }
        } catch (DataAccessException e) {
            log.warn("DB update failed for {} {}, restoring view count: {}", entity.getKey(), id, e.getMessage());
            longRedisTemplate.opsForValue().increment(redisKey, views);
            return;
        }

        Long remaining = longRedisTemplate.opsForValue().get(redisKey);
        if (remaining == null || remaining <= 0L) {
            stringRedisTemplate.opsForSet().remove(RedisKeys.DIRTY_VIEW_SET, redisKey);
        }
    }

    private static UUID parseIdFromKey(String redisKey) {
        int firstColon = redisKey.indexOf(':');
        if (firstColon < 0) return null;
        int secondColon = redisKey.indexOf(':', firstColon + 1);
        if (secondColon < 0) return null;
        String idStr = redisKey.substring(secondColon + 1);
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in key: {}", redisKey);
            return null;
        }
    }

    private static ViewEntity extractEntity(String redisKey) {
        int firstColon = redisKey.indexOf(':');
        if (firstColon < 0) return null;
        int secondColon = redisKey.indexOf(':', firstColon + 1);
        if (secondColon < 0) return null;
        String key = redisKey.substring(firstColon + 1, secondColon);
        for (ViewEntity e : ViewEntity.values()) {
            if (e.getKey().equals(key)) {
                return e;
            }
        }
        log.warn("Unknown entity key in Redis: {}", key);
        return null;
    }
}
