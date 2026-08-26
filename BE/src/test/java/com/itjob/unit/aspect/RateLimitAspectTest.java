package com.itjob.unit.aspect;

import com.itjob.annotation.RateLimit;
import com.itjob.aspect.RateLimitAspect;
import com.itjob.enums.RateLimitType;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - RateLimitAspect")
class RateLimitAspectTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private HttpServletRequest request;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(rateLimitService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("IP type -> identifier from X-Forwarded-For first entry")
    void ipTypeUsesForwardedForFirstEntry() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("loginIp");
        when(joinPoint.proceed()).thenReturn("ok");
        when(rateLimitService.tryAcquire("login", "1.2.3.4", 5, 60)).thenReturn(true);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act
        Object result = aspect.checkRateLimit(joinPoint, lock);

        // Assert
        assertThat(result).isEqualTo("ok");
        verify(rateLimitService).tryAcquire("login", "1.2.3.4", 5, 60);
    }

    @Test
    @DisplayName("IP type -> blocked throws TOO_MANY_REQUESTS without proceeding")
    void ipTypeBlockedThrows() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("loginIp");
        when(rateLimitService.tryAcquire(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act & Assert
        assertThatThrownBy(() -> aspect.checkRateLimit(joinPoint, lock))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("IP type -> falls back to X-Real-IP when X-Forwarded-For is absent")
    void ipTypeFallsBackToRealIp() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("loginIp");
        when(joinPoint.proceed()).thenReturn("ok");
        when(rateLimitService.tryAcquire(anyString(), eq("10.0.0.1"), anyInt(), anyInt())).thenReturn(true);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act
        aspect.checkRateLimit(joinPoint, lock);

        // Assert
        verify(rateLimitService).tryAcquire(anyString(), eq("10.0.0.1"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("IP type -> falls back to remote address when no proxy headers")
    void ipTypeFallsBackToRemoteAddr() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("loginIp");
        when(joinPoint.proceed()).thenReturn("ok");
        when(rateLimitService.tryAcquire(anyString(), eq("192.168.1.1"), anyInt(), anyInt())).thenReturn(true);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act
        aspect.checkRateLimit(joinPoint, lock);

        // Assert
        verify(rateLimitService).tryAcquire(anyString(), eq("192.168.1.1"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("USER type -> identifier from authenticated principal name")
    void userTypeUsesAuthenticatedName() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("applyUser");
        when(joinPoint.proceed()).thenReturn("ok");
        when(rateLimitService.tryAcquire("apply", "user@example.com", 3, 60)).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Act
        aspect.checkRateLimit(joinPoint, lock);

        // Assert
        verify(rateLimitService).tryAcquire("apply", "user@example.com", 3, 60);
    }

    @Test
    @DisplayName("USER type -> anonymous falls back to client IP")
    void userTypeAnonymousFallsBackToIp() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("applyUser");
        when(joinPoint.proceed()).thenReturn("ok");
        when(rateLimitService.tryAcquire(anyString(), eq("9.9.9.9"), anyInt(), anyInt())).thenReturn(true);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        // Act
        aspect.checkRateLimit(joinPoint, lock);

        // Assert
        verify(rateLimitService).tryAcquire(anyString(), eq("9.9.9.9"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("USER type -> no authentication falls back to client IP")
    void userTypeNoAuthFallsBackToIp() throws Throwable {
        // Arrange
        RateLimit lock = rateLimitOf("applyUser");
        when(joinPoint.proceed()).thenReturn("ok");
        when(rateLimitService.tryAcquire(anyString(), eq("7.7.7.7"), anyInt(), anyInt())).thenReturn(true);
        when(request.getHeader("X-Forwarded-For")).thenReturn("7.7.7.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act
        aspect.checkRateLimit(joinPoint, lock);

        // Assert
        verify(rateLimitService).tryAcquire(anyString(), eq("7.7.7.7"), anyInt(), anyInt());
    }

    private RateLimit rateLimitOf(String methodName) throws NoSuchMethodException {
        return RateLimitedService.class.getMethod(methodName).getAnnotation(RateLimit.class);
    }

    @SuppressWarnings("unused")
    private static class RateLimitedService {

        @RateLimit(key = "login", limit = 5, duration = 60, type = RateLimitType.IP)
        public void loginIp() {
        }

        @RateLimit(key = "apply", limit = 3, duration = 60, type = RateLimitType.USER)
        public void applyUser() {
        }
    }
}
