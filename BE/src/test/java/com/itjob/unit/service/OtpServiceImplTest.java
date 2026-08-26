package com.itjob.unit.service;

import com.itjob.constant.OtpConstant;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.service.OtpService;
import com.itjob.service.impl.OtpServiceImpl;
import com.itjob.util.HashUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - OtpServiceImpl")
class OtpServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("generateAndStore generates OTP and stores hash in Redis")
    void generateAndStore_storesOtpInRedis() {
        // Arrange
        String email = "test@example.com";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOps.increment(anyString())).thenReturn(1L);

        // Act
        String otp = otpService.generateAndStore(email);

        // Assert
        assertThat(otp).hasSize(OtpConstant.OTP_LENGTH);
        verify(valueOps).set(
                eq(RedisKeys.otp(email)),
                anyString(),
                eq(OtpConstant.OTP_TTL));
        verify(valueOps).set(RedisKeys.otpCooldown(email), "1", OtpConstant.COOLDOWN_TTL);
    }

    @Test
    @DisplayName("verify returns true for valid OTP")
    void verify_validOtp_returnsTrue() {
        // Arrange
        String email = "test@example.com";
        String otp = "123456";
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(valueOps.get(RedisKeys.otp(email))).thenReturn(HashUtil.sha256(otp));

        // Act
        boolean result = otpService.verify(email, otp);

        // Assert
        assertThat(result).isTrue();
        verify(redisTemplate).delete(RedisKeys.otp(email));
        verify(redisTemplate).delete(RedisKeys.otpAttempt(email));
    }

    @Test
    @DisplayName("verify returns false for invalid OTP length")
    void verify_invalidOtpLength_returnsFalse() {
        // Arrange
        String email = "test@example.com";
        String otp = "short";

        // Act
        boolean result = otpService.verify(email, otp);

        // Assert
        assertThat(result).isFalse();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("verify returns false when OTP not found in Redis")
    void verify_otpNotFound_returnsFalse() {
        // Arrange
        String email = "test@example.com";
        String otp = "123456";
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(valueOps.get(anyString())).thenReturn(null);

        // Act
        boolean result = otpService.verify(email, otp);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("delete deletes OTP keys from Redis")
    void delete_deletesOtpKeysFromRedis() {
        // Arrange
        String email = "test@example.com";

        // Act
        otpService.delete(email);

        // Assert
        verify(redisTemplate).delete(RedisKeys.otp(email));
        verify(redisTemplate).delete(RedisKeys.otpAttempt(email));
    }

    @Test
    @DisplayName("generateAndStore throws TOO_MANY_REQUESTS when cooldown exists")
    void generateAndStore_cooldownExists_throws() {
        // Arrange
        String email = "test@example.com";
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> otpService.generateAndStore(email))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }
}
