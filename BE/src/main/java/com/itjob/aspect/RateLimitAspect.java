package com.itjob.aspect;

import com.itjob.annotation.RateLimit;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identifier = resolveIdentifier(rateLimit);
        boolean allowed = rateLimitService.tryAcquire(rateLimit.key(), identifier, rateLimit.limit(), rateLimit.duration());
        if (!allowed) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return joinPoint.proceed();
    }

    private String resolveIdentifier(RateLimit rateLimit) {
        return switch (rateLimit.type()) {
            case IP -> {
                HttpServletRequest request = ((ServletRequestAttributes)
                        RequestContextHolder.currentRequestAttributes()).getRequest();
                yield getClientIp(request);
            }
            case USER -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null
                        && auth.isAuthenticated()
                        && !(auth instanceof AnonymousAuthenticationToken)
                        && auth.getPrincipal() != null) {
                    yield auth.getName();
                }
                HttpServletRequest request = ((ServletRequestAttributes)
                        RequestContextHolder.currentRequestAttributes()).getRequest();
                yield getClientIp(request);
            }
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
