package com.itjob.service.impl;

import com.itjob.constant.OtpConstant;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.service.OtpService;
import com.itjob.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();


    private final StringRedisTemplate redisTemplate;

    @Override
    public String generateAndStore(String email) {
        String cooldownKey = RedisKeys.otpCooldown(email);
        if (redisTemplate.hasKey(cooldownKey)) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        String sendLimitKey = RedisKeys.otpSendLimit(email);
        Long sent = redisTemplate.opsForValue().increment(sendLimitKey);
        if (sent == null) {
            sent = 1L;
        }
        if (sent == 1) {
            redisTemplate.expire(sendLimitKey, OtpConstant.SEND_LIMIT_TTL);
        }
        if (sent > OtpConstant.MAX_SENDS) {
            log.warn("OTP send limit reached for email: {}", email);
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        int min = (int) Math.pow(10, OtpConstant.OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OtpConstant.OTP_LENGTH) - 1;
        int otp = RANDOM.nextInt(max - min + 1) + min;
        String otpStr = String.valueOf(otp);
        String hashed = HashUtil.sha256(otpStr);

        redisTemplate.opsForValue().set(
                RedisKeys.otp(email),
                hashed,
                OtpConstant.OTP_TTL
        );

        redisTemplate.opsForValue().set(cooldownKey, "1", OtpConstant.COOLDOWN_TTL);

        log.debug("OTP generated for email: {}", email);
        return otpStr;
    }

    @Override
    public boolean verify(String email, String otp) {
        String attemptKey = RedisKeys.otpAttempt(email);

        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts == null) {
            attempts = 1L;
        }

        if (attempts == 1) {
            redisTemplate.expire(attemptKey, OtpConstant.ATTEMPT_TTL);
        }

        if (attempts > OtpConstant.MAX_ATTEMPTS) {
            log.warn("Too many OTP attempts for email: {}", email);
            throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        String key = RedisKeys.otp(email);
        String storedHash = redisTemplate.opsForValue().get(key);

        if (storedHash == null) {
            log.debug("OTP not found or expired for email: {}", email);
            return false;
        }

        String inputHash = HashUtil.sha256(otp);
        boolean matched = storedHash.equals(inputHash);

        if (matched) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
            log.debug("OTP verified successfully for email: {}", email);
        } else {
            log.debug("OTP mismatch for email: {}", email);
        }

        return matched;
    }

    @Override
    public void delete(String email) {
        redisTemplate.delete(RedisKeys.otp(email));
        log.debug("OTP deleted for email: {}", email);
    }

}
