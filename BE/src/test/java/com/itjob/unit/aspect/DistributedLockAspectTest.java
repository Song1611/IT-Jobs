package com.itjob.unit.aspect;

import com.itjob.annotation.DistributedLock;
import com.itjob.aspect.DistributedLockAspect;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.service.DistributedLockService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - DistributedLockAspect")
class DistributedLockAspectTest {

    @Mock
    private DistributedLockService lockService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private DistributedLockAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new DistributedLockAspect(lockService);
    }

    @Test
    @DisplayName("acquires lock -> proceeds and unlocks with SpEL-resolved key")
    void acquiresLockProceedsAndUnlocks() throws Throwable {
        // Arrange
        UUID id = UUID.randomUUID();
        DistributedLock lock = lockOf("doWork", UUID.class);
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("doWork", UUID.class));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{id});
        when(joinPoint.proceed()).thenReturn("done");
        when(lockService.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);

        // Act
        Object result = aspect.handleLock(joinPoint, lock);

        // Assert
        assertThat(result).isEqualTo("done");
        InOrder order = inOrder(lockService, joinPoint);
        order.verify(lockService).tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS));
        order.verify(joinPoint).proceed();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        order.verify(lockService).unlock(keyCaptor.capture(), valueCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(RedisKeys.lockKey("test-" + id));
        assertThat(valueCaptor.getValue()).isNotBlank();
    }

    @Test
    @DisplayName("lock not acquired and no wait -> throws RESOURCE_BUSY without proceeding")
    void lockNotAcquiredThrowsResourceBusy() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("doWork", UUID.class);
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("doWork", UUID.class));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{UUID.randomUUID()});
        when(lockService.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> aspect.handleLock(joinPoint, lock))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_BUSY);
        verify(joinPoint, never()).proceed();
        verify(lockService, never()).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("leaseTime <= 0 -> falls back to default 30s lease")
    void zeroLeaseTimeUsesDefault() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("noLease");
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("noLease"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(lockService.tryLock(anyString(), anyString(), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        aspect.handleLock(joinPoint, lock);

        // Assert
        verify(lockService).tryLock(anyString(), anyString(), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("target throws -> lock still released in finally")
    void targetExceptionStillUnlocks() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("doWork", UUID.class);
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("doWork", UUID.class));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{UUID.randomUUID()});
        when(lockService.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

        // Act & Assert
        assertThatThrownBy(() -> aspect.handleLock(joinPoint, lock))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
        verify(lockService).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("waitTime > 0 -> retries until lock acquired, then proceeds and unlocks")
    void retriesUntilAcquired() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("withWait");
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("withWait"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(lockService.tryLock(anyString(), anyString(), eq(5L), eq(TimeUnit.SECONDS)))
                .thenReturn(false, true);
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        aspect.handleLock(joinPoint, lock);

        // Assert
        verify(lockService, times(2)).tryLock(anyString(), anyString(), eq(5L), eq(TimeUnit.SECONDS));
        verify(joinPoint).proceed();
        verify(lockService).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("lock never acquired within wait time -> throws RESOURCE_BUSY without proceeding")
    void retryExhaustedThrowsResourceBusy() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("withWait");
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("withWait"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(lockService.tryLock(anyString(), anyString(), eq(5L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> aspect.handleLock(joinPoint, lock))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_BUSY);
        verify(joinPoint, never()).proceed();
        verify(lockService, never()).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("lock service failure -> propagates without proceeding or unlocking (fail closed)")
    void lockServiceFailurePropagates() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("doWork", UUID.class);
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("doWork", UUID.class));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{UUID.randomUUID()});
        when(lockService.tryLock(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                .thenThrow(new DataAccessException("redis down") {});

        // Act & Assert
        assertThatThrownBy(() -> aspect.handleLock(joinPoint, lock))
                .isInstanceOf(DataAccessException.class);
        verify(joinPoint, never()).proceed();
        verify(lockService, never()).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("blank key expression -> throws INVALID_KEY without locking")
    void blankKeyThrowsInvalidKey() throws Throwable {
        // Arrange
        DistributedLock lock = lockOf("emptyKey");
        when(signature.getMethod()).thenReturn(LockedService.class.getMethod("emptyKey"));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        // Act & Assert
        assertThatThrownBy(() -> aspect.handleLock(joinPoint, lock))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_KEY);
        verify(lockService, never()).tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verify(joinPoint, never()).proceed();
    }

    private DistributedLock lockOf(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        return LockedService.class.getMethod(methodName, paramTypes).getAnnotation(DistributedLock.class);
    }

    @SuppressWarnings("unused")
    private static class LockedService {

        @DistributedLock(key = "'test-' + #id")
        public String doWork(UUID id) {
            return id.toString();
        }

        @DistributedLock(key = "'no-lease'", leaseTime = 0)
        public void noLease() {
        }

        @DistributedLock(key = "'retry'", waitTime = 1, leaseTime = 5)
        public void withWait() {
        }

        @DistributedLock(key = "''")
        public void emptyKey() {
        }
    }
}
