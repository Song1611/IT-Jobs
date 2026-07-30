package com.itjob.service.impl;

import com.itjob.constant.SearchSuggestionConstant;
import com.itjob.redis.RedisKeys;
import com.itjob.service.SearchSuggestionService;
import com.itjob.util.RedisOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void recordKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        keyword = keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (keyword.length() > SearchSuggestionConstant.MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, SearchSuggestionConstant.MAX_KEYWORD_LENGTH);
        }

        String finalKeyword = keyword;
        RedisOperation.run(() -> {
            int maxLen = Math.min(finalKeyword.length(), SearchSuggestionConstant.MAX_PREFIX_LENGTH);
            for (int i = 1; i <= maxLen; i++) {
                String prefix = finalKeyword.substring(0, i);
                String key = RedisKeys.suggestPrefixKey(prefix);
                Boolean keyExists = stringRedisTemplate.hasKey(key);
                stringRedisTemplate.opsForZSet().incrementScore(key, finalKeyword, 1);
                if (!keyExists) {
                    stringRedisTemplate.expire(key, SearchSuggestionConstant.SUGGEST_TTL);
                }
            }
        }, "Failed to record search keyword: {}", keyword);
    }

    @Override
    public void removeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        keyword = keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        String finalKeyword = keyword;
        RedisOperation.run(() -> {
            int maxLen = Math.min(finalKeyword.length(), SearchSuggestionConstant.MAX_PREFIX_LENGTH);
            for (int i = 1; i <= maxLen; i++) {
                String prefix = finalKeyword.substring(0, i);
                stringRedisTemplate.opsForZSet().remove(
                        RedisKeys.suggestPrefixKey(prefix), finalKeyword);
            }
        }, "Failed to remove search keyword: {}", keyword);
    }

    @Override
    public List<String> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();

        prefix = prefix.trim().toLowerCase(Locale.ROOT);
        if (prefix.length() > SearchSuggestionConstant.MAX_PREFIX_LENGTH) {
            prefix = prefix.substring(0, SearchSuggestionConstant.MAX_PREFIX_LENGTH);
        }

        int finalLimit = Math.min(limit, SearchSuggestionConstant.MAX_RESULTS);
        String finalPrefix = prefix;

        return RedisOperation.supply(() -> {
            Set<String> result = stringRedisTemplate.opsForZSet().reverseRange(
                    RedisKeys.suggestPrefixKey(finalPrefix), 0, finalLimit - 1);
            return result != null ? List.copyOf(result) : List.of();
        }, "Failed to get suggestions for prefix: {}", prefix);
    }
}
