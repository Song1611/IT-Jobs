package com.itjob.unit.service;

import com.itjob.service.impl.DistributedLockServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - DistributedLockServiceImpl")
class DistributedLockServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DistributedLockServiceImpl distributedLockService;

    @Test
    @DisplayName("tryLock -> true when SET NX succeeds")
    void tryLockAcquired() {
        // Arrange
        String key = "lock:job:1";
        String value = "instance-1";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(key, value, 30L, TimeUnit.SECONDS)).thenReturn(true);

        // Act
        boolean locked = distributedLockService.tryLock(key, value, 30L, TimeUnit.SECONDS);

        // Assert
        assertThat(locked).isTrue();
        verify(valueOperations).setIfAbsent(key, value, 30L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("tryLock -> false when key already held")
    void tryLockNotAcquired() {
        // Arrange
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS))
                .thenReturn(false);

        // Act
        boolean locked = distributedLockService.tryLock("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS);

        // Assert
        assertThat(locked).isFalse();
        verify(valueOperations).setIfAbsent("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("tryLock -> false when Redis returns null")
    void tryLockReturnsFalseWhenRedisReturnsNull() {
        // Arrange
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS))
                .thenReturn(null);

        // Act
        boolean locked = distributedLockService.tryLock("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS);

        // Assert
        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("tryLock -> false on redis failure (fail closed)")
    void tryLockRedisFailureFailsClosed() {
        // Arrange
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS))
                .thenThrow(new RuntimeException("redis down"));

        // Act
        boolean locked = distributedLockService.tryLock("lock:job:1", "instance-1", 30L, TimeUnit.SECONDS);

        // Assert
        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("unlock -> executes script with key and value")
    void unlockExecutesScriptWithKeyAndValue() {
        // Arrange
        String key = "lock:job:1";
        String value = "instance-1";
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(1L);

        // Act
        distributedLockService.unlock(key, value);

        // Assert
        verify(stringRedisTemplate).execute(any(), eq(List.of(key)), eq(value));
    }

    @Test
    @DisplayName("unlock script returns 0 (lock expired) -> no exception")
    void unlockAlreadyExpiredDoesNotThrow() {
        // Arrange
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(0L);

        // Act & Assert
        assertThatCode(() -> distributedLockService.unlock("lock:job:1", "instance-1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("unlock redis failure -> swallowed, no exception")
    void unlockRedisFailureDoesNotThrow() {
        // Arrange
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("redis down"));

        // Act & Assert
        assertThatCode(() -> distributedLockService.unlock("lock:job:1", "instance-1"))
                .doesNotThrowAnyException();
    }
}
