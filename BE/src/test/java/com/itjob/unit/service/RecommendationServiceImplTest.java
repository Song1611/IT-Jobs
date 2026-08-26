package com.itjob.unit.service;

import com.itjob.constant.RecommendationConstant;
import com.itjob.entity.Application;
import com.itjob.entity.Job;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.enums.JobStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.RecentViewService;
import com.itjob.service.TrendingJobService;
import com.itjob.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit - RecommendationServiceImpl")
class RecommendationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RecentViewService recentViewService;

    @Mock
    private TrendingJobService trendingJobService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    @DisplayName("getRecommendedJobs cache hit -> returns cached ids without regenerating")
    void cacheHitReturnsCachedIds() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(RedisKeys.recommendKey(userId), 0, 4L))
                .thenReturn(List.of(id1.toString(), id2.toString()));

        // Act
        List<UUID> result = recommendationService.getRecommendedJobs(userId, 5);

        // Assert
        assertThat(result).containsExactly(id1, id2);
        verifyNoInteractions(userRepository, jobRepository, applicationRepository,
                recentViewService, trendingJobService);
    }

    @Test
    @DisplayName("getRecommendedJobs cache miss + user missing -> throws USER_NOT_FOUND")
    void cacheMissUserNotFoundThrows() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(RedisKeys.recommendKey(userId), 0, 4L)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getRecommendedJobs(userId, 5))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getRecommendedJobs no open jobs -> empty result, no caching")
    void noCandidatesReturnsEmpty() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(RedisKeys.recommendKey(userId), 0, 4L)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(applicationRepository.findByUserId(userId)).thenReturn(List.of());
        when(recentViewService.getRecentViewIds(userId, RecommendationConstant.RECENT_JOB_LIMIT))
                .thenReturn(List.of());
        when(trendingJobService.getTopJobIds(RecommendationConstant.TRENDING_JOB_LIMIT))
                .thenReturn(List.of());
        when(jobRepository.findLatestOpenJobs(eq(JobStatus.OPEN.getValue()), any(Pageable.class)))
                .thenReturn(List.of());

        // Act
        List<UUID> result = recommendationService.getRecommendedJobs(userId, 5);

        // Assert
        assertThat(result).isEmpty();
        verify(stringRedisTemplate, never()).delete(anyString());
        verify(stringRedisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("getRecommendedJobs cache miss -> ranks by score, excludes applied jobs, caches result")
    void cacheMissGeneratesAndCachesRankedJobs() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID skillMatchTrending = UUID.randomUUID();
        UUID skillMatchOnly = UUID.randomUUID();
        UUID trendingOnly = UUID.randomUUID();
        UUID appliedJobId = UUID.randomUUID();

        User user = user(userId);

        Job best = job(skillMatchTrending, "fulltime", "Ho Chi Minh", "Java");
        Job medium = job(skillMatchOnly, "fulltime", "Ha Noi", "Java");
        Job applied = job(appliedJobId, "fulltime", "Ha Noi", "Java");
        Job trending = job(trendingOnly, "contract", "Da Nang", "Go");

        Application application = Application.builder().job(applied).build();

        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(RedisKeys.recommendKey(userId), 0, 4L)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserId(userId)).thenReturn(List.of(application));
        when(recentViewService.getRecentViewIds(userId, RecommendationConstant.RECENT_JOB_LIMIT))
                .thenReturn(List.of());
        when(trendingJobService.getTopJobIds(RecommendationConstant.TRENDING_JOB_LIMIT))
                .thenReturn(List.of(skillMatchTrending, trendingOnly));
        when(jobRepository.findLatestOpenJobs(eq(JobStatus.OPEN.getValue()), any(Pageable.class)))
                .thenReturn(List.of(best, medium, applied, trending));

        // Act
        List<UUID> result = recommendationService.getRecommendedJobs(userId, 5);

        // Assert
        assertThat(result).containsExactly(skillMatchTrending, skillMatchOnly, trendingOnly);

        String key = RedisKeys.recommendKey(userId);
        InOrder order = inOrder(stringRedisTemplate, listOperations);
        order.verify(stringRedisTemplate).delete(key);
        order.verify(listOperations).rightPushAll(
                key,
                skillMatchTrending.toString(),
                skillMatchOnly.toString(),
                trendingOnly.toString());
        order.verify(stringRedisTemplate).expire(key, RecommendationConstant.CACHE_TTL);
    }

    @Test
    @DisplayName("getRecommendedJobs limit=1 -> reads range 0..0 and returns only the top recommendation")
    void limitOneReturnsSingleTopRecommendation() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID top = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        User user = user(userId);

        Job best = job(top, "fulltime", "Ho Chi Minh", "Java");
        Job other = job(second, "fulltime", "Ha Noi", "Java");

        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(RedisKeys.recommendKey(userId), 0, 0L)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserId(userId)).thenReturn(List.of());
        when(recentViewService.getRecentViewIds(userId, RecommendationConstant.RECENT_JOB_LIMIT))
                .thenReturn(List.of());
        when(trendingJobService.getTopJobIds(RecommendationConstant.TRENDING_JOB_LIMIT))
                .thenReturn(List.of());
        when(jobRepository.findLatestOpenJobs(eq(JobStatus.OPEN.getValue()), any(Pageable.class)))
                .thenReturn(List.of(best, other));

        // Act
        List<UUID> result = recommendationService.getRecommendedJobs(userId, 1);

        // Assert
        assertThat(result).containsExactly(top);
        verify(listOperations).range(RedisKeys.recommendKey(userId), 0, 0L);
    }

    @Test
    @DisplayName("invalidateCache -> deletes the user recommendation key")
    void invalidateCacheDeletesKey() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        recommendationService.invalidateCache(userId);

        // Assert
        verify(stringRedisTemplate).delete(RedisKeys.recommendKey(userId));
    }

    private static Skill skill(String name) {
        return Skill.builder().name(name).build();
    }

    private static User user(UUID userId) {
        return User.builder()
                .id(userId)
                .address("Ho Chi Minh")
                .skills(Set.of(skill("Java")))
                .build();
    }

    private static Job job(UUID id, String type, String workLocation, String... skills) {
        Set<Skill> skillSet = Arrays.stream(skills)
                .map(RecommendationServiceImplTest::skill)
                .collect(Collectors.toSet());
        return Job.builder().id(id).type(type).workLocation(workLocation).skills(skillSet).build();
    }
}
