package com.itjob.aspect;

import com.itjob.annotation.DistributedLock;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final long RETRY_INTERVAL_MS = 100;

    private final DistributedLockService lockService;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramNames = new DefaultParameterNameDiscoverer();

    @Around("@annotation(lock)")
    public Object handleLock(ProceedingJoinPoint joinPoint, DistributedLock lock) throws Throwable {
        String rawKey = evaluateKeySafely(joinPoint, lock.key());
        String lockKey = RedisKeys.lockKey(rawKey);
        String lockValue = UUID.randomUUID().toString();

        long leaseTime = lock.leaseTime() > 0 ? lock.leaseTime() : 30L;
        if (lock.leaseTime() <= 0) {
            log.warn("leaseTime <= 0, using default 30s for {}", lockKey);
        }
        long endTime = lock.waitTime() > 0
                ? System.nanoTime() + TimeUnit.SECONDS.toNanos(lock.waitTime())
                : 0;

        boolean acquired = lockService.tryLock(lockKey, lockValue, leaseTime, TimeUnit.SECONDS);

        while (!acquired && lock.waitTime() > 0 && System.nanoTime() < endTime) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(RETRY_INTERVAL_MS));
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            acquired = lockService.tryLock(lockKey, lockValue, leaseTime, TimeUnit.SECONDS);
        }

        if (!acquired) {
            log.warn("Distributed lock acquisition failed for key: {}", lockKey);
            throw new AppException(ErrorCode.RESOURCE_BUSY);
        }

        try {
            return joinPoint.proceed();
        } finally {
            lockService.unlock(lockKey, lockValue);
        }
    }

    private String evaluateKeySafely(ProceedingJoinPoint joinPoint, String keyExpr) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String[] paramNamesArray = paramNames.getParameterNames(sig.getMethod());
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext ctx = new StandardEvaluationContext();
            if (paramNamesArray != null) {
                for (int i = 0; i < paramNamesArray.length; i++) {
                    ctx.setVariable(paramNamesArray[i], args[i]);
                }
            }

            Expression expr = parser.parseExpression(keyExpr);
            String result = expr.getValue(ctx, String.class);
            if (result == null || result.isBlank()) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            return result;
        } catch (EvaluationException e) {
            log.error("Failed to evaluate lock key expression '{}': {}", keyExpr, e.getMessage());
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
