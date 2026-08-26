package com.itjob.unit.service;

import com.itjob.redis.RedisKeys;
import com.itjob.service.impl.RateLimitServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - RateLimitServiceImpl")
class RateLimitServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    @Test
    @DisplayName("tryAcquire count <= limit -> allowed and passes key + args to Lua script")
    void withinLimitAllowed() {
        // Arrange
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(5L);

        // Act
        boolean allowed = rateLimitService.tryAcquire("login", "1.2.3.4", 5, 60);

        // Assert
        assertThat(allowed).isTrue();
        verify(stringRedisTemplate).execute(
                any(),
                eq(List.of(RedisKeys.rateLimitKey("login", "1.2.3.4"))),
                eq("5"), eq("60"));
    }

    @Test
    @DisplayName("tryAcquire count > limit -> blocked")
    void overLimitBlocked() {
        // Arrange
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(6L);

        // Act
        boolean allowed = rateLimitService.tryAcquire("login", "1.2.3.4", 5, 60);

        // Assert
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("tryAcquire redis failure -> fails open (allows request)")
    void redisErrorFailsOpen() {
        // Arrange
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("redis down"));

        // Act
        boolean allowed = rateLimitService.tryAcquire("login", "1.2.3.4", 5, 60);

        // Assert
        assertThat(allowed).isTrue();
    }
}
