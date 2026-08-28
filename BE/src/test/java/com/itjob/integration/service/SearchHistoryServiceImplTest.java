package com.itjob.integration.service;

import com.itjob.redis.RedisKeys;
import com.itjob.service.SearchHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("IT - SearchHistoryService")
class SearchHistoryServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("recordSearch -> stores the normalized keyword in Redis")
    void recordSearchStoresKeyword() {
        UUID userId = UUID.randomUUID();

        searchHistoryService.recordSearch(userId, "  Java  DEVELOPER ");

        List<String> stored = stringRedisTemplate.opsForList()
                .range(RedisKeys.searchHistoryKey(userId), 0, -1);
        assertThat(stored).containsExactly("Java DEVELOPER");
    }

    @Test
    @DisplayName("recordSearch -> ignores blank keywords and null user")
    void recordSearchBlankDoesNothing() {
        UUID userId = UUID.randomUUID();

        searchHistoryService.recordSearch(userId, "   ");
        searchHistoryService.recordSearch(null, "java");

        assertThat(stringRedisTemplate.hasKey(RedisKeys.searchHistoryKey(userId))).isFalse();
    }

    @Test
    @DisplayName("getSearchHistory -> returns recent searches newest first")
    void getSearchHistoryReturnsRecentFirst() {
        UUID userId = UUID.randomUUID();
        searchHistoryService.recordSearch(userId, "java");
        searchHistoryService.recordSearch(userId, "spring");

        List<String> result = searchHistoryService.getSearchHistory(userId, 10);

        assertThat(result).containsExactly("spring", "java");
    }

    @Test
    @DisplayName("getSearchHistory -> returns empty for a non-positive limit")
    void getSearchHistoryNonPositiveLimitReturnsEmpty() {
        UUID userId = UUID.randomUUID();
        searchHistoryService.recordSearch(userId, "java");

        assertThat(searchHistoryService.getSearchHistory(userId, 0)).isEmpty();
        assertThat(searchHistoryService.getSearchHistory(userId, -1)).isEmpty();
    }
}