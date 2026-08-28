package com.itjob.integration.service;

import com.itjob.redis.RedisKeys;
import com.itjob.service.RecentViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("IT - RecentViewService")
class RecentViewServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private RecentViewService recentViewService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("recordView -> stores the job id in Redis, newest first")
    void recordViewStoresJobId() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        recentViewService.recordView(userId, jobId);

        List<String> stored = stringRedisTemplate.opsForList()
                .range(RedisKeys.recentViewKey(userId), 0, -1);
        assertThat(stored).containsExactly(jobId.toString());
    }

    @Test
    @DisplayName("recordView -> ignores null user or job")
    void recordViewNullDoesNothing() {
        UUID userId = UUID.randomUUID();

        recentViewService.recordView(null, UUID.randomUUID());
        recentViewService.recordView(userId, null);

        assertThat(stringRedisTemplate.hasKey(RedisKeys.recentViewKey(userId))).isFalse();
    }

    @Test
    @DisplayName("getRecentViewIds -> returns parsed job ids newest first")
    void getRecentViewIdsReturnsIds() {
        UUID userId = UUID.randomUUID();
        UUID job1 = UUID.randomUUID();
        UUID job2 = UUID.randomUUID();
        recentViewService.recordView(userId, job1);
        recentViewService.recordView(userId, job2);

        List<UUID> result = recentViewService.getRecentViewIds(userId, 10);

        assertThat(result).containsExactly(job2, job1);
    }

    @Test
    @DisplayName("getRecentViewIds -> returns empty for a non-positive limit")
    void getRecentViewIdsNonPositiveLimitReturnsEmpty() {
        UUID userId = UUID.randomUUID();
        recentViewService.recordView(userId, UUID.randomUUID());

        assertThat(recentViewService.getRecentViewIds(userId, 0)).isEmpty();
        assertThat(recentViewService.getRecentViewIds(userId, -1)).isEmpty();
    }
}