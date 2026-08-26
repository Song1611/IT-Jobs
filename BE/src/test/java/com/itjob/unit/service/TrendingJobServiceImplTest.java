package com.itjob.unit.service;

import com.itjob.constant.TrendingConstant;
import com.itjob.redis.RedisKeys;
import com.itjob.service.impl.TrendingJobServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - TrendingJobServiceImpl")
class TrendingJobServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private TrendingJobServiceImpl trendingJobService;

    @Test
    @DisplayName("recordScore -> increments score and sets TTL when key is new")
    void recordScoreSetsTtlOnNewKey() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String key = RedisKeys.trendingDailyKey();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.hasKey(key)).thenReturn(false);

        // Act
        trendingJobService.recordScore(jobId, 2.5);

        // Assert
        verify(stringRedisTemplate).hasKey(key);
        verify(zSetOperations).incrementScore(key, jobId.toString(), 2.5);
        verify(stringRedisTemplate).expire(key, TrendingConstant.TTL_HOURS, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("recordScore -> skips TTL when key already exists")
    void recordScoreSkipsTtlOnExistingKey() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String key = RedisKeys.trendingDailyKey();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.hasKey(key)).thenReturn(true);

        // Act
        trendingJobService.recordScore(jobId, 1.0);

        // Assert
        verify(stringRedisTemplate).hasKey(key);
        verify(zSetOperations).incrementScore(key, jobId.toString(), 1.0);
        verify(stringRedisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("recordScore -> ignores Redis failure")
    void recordScoreRedisFailureDoesNotThrow() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String key = RedisKeys.trendingDailyKey();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.incrementScore(key, jobId.toString(), 2.5))
                .thenThrow(new DataAccessException("redis down") {});

        // Act & Assert
        assertThatCode(() -> trendingJobService.recordScore(jobId, 2.5))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getTopJobIds -> returns parsed UUIDs in ranking order")
    void getTopJobIdsParsesUuids() {
        // Arrange
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        String key = RedisKeys.trendingDailyKey();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(key, 0L, 4L))
                .thenReturn(new LinkedHashSet<>(List.of(jobId1.toString(), jobId2.toString())));

        // Act
        List<UUID> result = trendingJobService.getTopJobIds(5);

        // Assert
        assertThat(result).containsExactly(jobId1, jobId2);
        verify(zSetOperations).reverseRange(key, 0L, 4L);
    }

    @Test
    @DisplayName("getTopJobIds -> empty list when redis returns null")
    void getTopJobIdsRedisNullReturnsEmpty() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), anyLong(), anyLong())).thenReturn(null);

        // Act
        List<UUID> result = trendingJobService.getTopJobIds(5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopJobIds -> filters out invalid UUID members")
    void getTopJobIdsFiltersInvalidUuids() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .thenReturn(Set.of(jobId.toString(), "not-a-uuid"));

        // Act
        List<UUID> result = trendingJobService.getTopJobIds(5);

        // Assert
        assertThat(result).containsExactly(jobId);
    }

    @Test
    @DisplayName("getTopJobIds -> returns empty when Redis fails (fail open)")
    void getTopJobIdsRedisFailureReturnsEmpty() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .thenThrow(new DataAccessException("redis down") {});

        // Act
        List<UUID> result = trendingJobService.getTopJobIds(5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopJobIds limit=1 -> reads range 0..0")
    void getTopJobIdsUsesLimitAsRangeEnd() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String key = RedisKeys.trendingDailyKey();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(key, 0L, 0L))
                .thenReturn(Set.of(jobId.toString()));

        // Act
        List<UUID> result = trendingJobService.getTopJobIds(1);

        // Assert
        assertThat(result).containsExactly(jobId);
        verify(zSetOperations).reverseRange(key, 0L, 0L);
    }

    @Test
    @DisplayName("removeJob -> removes member from today and yesterday keys")
    void removeJobRemovesFromTodayAndYesterday() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Act
        trendingJobService.removeJob(jobId);

        // Assert
        verify(zSetOperations).remove(RedisKeys.trendingDailyKey(), jobId.toString());
        verify(zSetOperations).remove(RedisKeys.trendingDailyKey(LocalDate.now().minusDays(1)), jobId.toString());
    }
}
