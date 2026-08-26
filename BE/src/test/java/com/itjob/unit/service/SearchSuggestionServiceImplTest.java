package com.itjob.unit.service;

import com.itjob.constant.SearchSuggestionConstant;
import com.itjob.service.impl.SearchSuggestionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - SearchSuggestionServiceImpl")
class SearchSuggestionServiceImplTest {

    private static final String KEYWORD = "java developer";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private SearchSuggestionServiceImpl searchSuggestionService;

    @Test
    @DisplayName("recordKeyword -> trims, collapses whitespace, lowercases and records every prefix")
    void recordKeywordNormalizesAndRecordsAllPrefixes() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        searchSuggestionService.recordKeyword("  Java  DEVELOPER  ");

        // Assert
        verify(zSetOperations, times(KEYWORD.length()))
                .incrementScore(anyString(), eq(KEYWORD), eq(1.0));
        verify(zSetOperations).incrementScore("suggest:j", KEYWORD, 1.0);
        verify(zSetOperations).incrementScore("suggest:java", KEYWORD, 1.0);
        verify(zSetOperations).incrementScore("suggest:java ", KEYWORD, 1.0);
        verify(zSetOperations).incrementScore("suggest:java developer", KEYWORD, 1.0);
        verify(stringRedisTemplate, times(KEYWORD.length()))
                .expire(anyString(), eq(SearchSuggestionConstant.SUGGEST_TTL));
    }

    @Test
    @DisplayName("recordKeyword -> no TTL when prefix key already exists")
    void recordKeywordExistingKeySkipsExpire() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        searchSuggestionService.recordKeyword("java");

        // Assert
        verify(zSetOperations, times(4)).incrementScore(anyString(), eq("java"), eq(1.0));
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("recordKeyword -> truncates keyword longer than max length")
    void recordKeywordTruncatesLongKeyword() {
        // Arrange
        String longKeyword = "a".repeat(150);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        searchSuggestionService.recordKeyword(longKeyword);

        // Assert
        String truncated = "a".repeat(SearchSuggestionConstant.MAX_KEYWORD_LENGTH);
        verify(zSetOperations, times(SearchSuggestionConstant.MAX_PREFIX_LENGTH))
                .incrementScore(anyString(), eq(truncated), eq(1.0));
    }

    @Test
    @DisplayName("recordKeyword -> blank/null keyword does nothing")
    void recordKeywordBlankDoesNothing() {
        // Act
        searchSuggestionService.recordKeyword(null);
        searchSuggestionService.recordKeyword("   ");

        // Assert
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("removeKeyword -> removes member from each prefix zset")
    void removeKeywordRemovesFromPrefixes() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Act
        searchSuggestionService.removeKeyword(" Java ");

        // Assert
        verify(zSetOperations).remove("suggest:j", "java");
        verify(zSetOperations).remove("suggest:ja", "java");
        verify(zSetOperations).remove("suggest:jav", "java");
        verify(zSetOperations).remove("suggest:java", "java");
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("getSuggestions -> returns ranked members for normalized prefix")
    void getSuggestionsReturnsResults() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("suggest:java", 0, 4L))
                .thenReturn(Set.of("java developer", "java lead"));

        // Act
        List<String> suggestions = searchSuggestionService.getSuggestions("  JAVA  ", 5);

        // Assert
        assertThat(suggestions).containsExactlyInAnyOrder("java developer", "java lead");
        verify(zSetOperations).reverseRange("suggest:java", 0, 4L);
    }

    @Test
    @DisplayName("getSuggestions -> caps limit at max results")
    void getSuggestionsCapsLimit() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), eq(0L), eq(9L)))
                .thenReturn(Set.of("a"));

        // Act
        searchSuggestionService.getSuggestions("java", 100);

        // Assert
        verify(zSetOperations).reverseRange("suggest:java", 0, 9L);
    }

    @Test
    @DisplayName("getSuggestions -> empty list when redis returns null")
    void getSuggestionsRedisNullReturnsEmpty() {
        // Arrange
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), any(Long.class), any(Long.class)))
                .thenReturn(null);

        // Act
        List<String> suggestions = searchSuggestionService.getSuggestions("java", 5);

        // Assert
        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("getSuggestions -> blank prefix returns empty without touching redis")
    void getSuggestionsBlankPrefixReturnsEmpty() {
        // Act
        assertThat(searchSuggestionService.getSuggestions(null, 5)).isEmpty();
        assertThat(searchSuggestionService.getSuggestions("   ", 5)).isEmpty();

        // Assert
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("getSuggestions -> truncates prefix longer than max prefix length")
    void getSuggestionsTruncatesLongPrefix() {
        // Arrange
        String longPrefix = "a".repeat(30);
        String truncated = "a".repeat(SearchSuggestionConstant.MAX_PREFIX_LENGTH);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(anyString(), eq(0L), eq(4L)))
                .thenReturn(Set.of("x"));

        // Act
        searchSuggestionService.getSuggestions(longPrefix, 5);

        // Assert
        verify(zSetOperations).reverseRange("suggest:" + truncated, 0, 4L);
    }
}
