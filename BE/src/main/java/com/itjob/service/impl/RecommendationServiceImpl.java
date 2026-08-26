package com.itjob.service.impl;

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
import com.itjob.service.RecommendationService;
import com.itjob.service.TrendingJobService;
import com.itjob.util.RedisOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final RecentViewService recentViewService;
    private final TrendingJobService trendingJobService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<UUID> getRecommendedJobs(UUID userId, int limit) {
        List<String> cached = RedisOperation.supply(() -> {
            String key = RedisKeys.recommendKey(userId);
            return stringRedisTemplate.opsForList().range(key, 0, (long)limit - 1);
        }, "Failed to read recommendation cache for user {}", userId);

        if (cached != null && !cached.isEmpty()) {
            return RedisOperation.parseUuids(cached);
        }

        List<UUID> jobs =
                generateRecommendations(
                        userId,
                        Math.min(limit,
                                RecommendationConstant.CACHE_MAX_RESULTS));

        if (!jobs.isEmpty()) {
            cacheRecommendations(userId, jobs);
        }

        return jobs;
    }

    @Override
    public void invalidateCache(UUID userId) {
        RedisOperation.run(() ->
                        stringRedisTemplate.delete(RedisKeys.recommendKey(userId)),
                "Failed to invalidate recommendation cache for user {}",
                userId);
    }

    private List<UUID> generateRecommendations(UUID userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Set<String> userSkillNames = collectUserSkillNames(user);
        String userAddress = user.getAddress() != null ? user.getAddress().toLowerCase(Locale.ROOT) : "";
        List<Application> userApplications = applicationRepository.findByUserId(userId);
        Set<String> appliedTypes = collectAppliedJobTypes(userApplications);
        Set<UUID> appliedJobIds = collectAppliedJobIds(userApplications);
        Set<UUID> recentJobIds = collectRecentJobIds(userId);
        Set<UUID> trendingJobIds = collectTrendingJobIds();

        List<Job> candidates = jobRepository.findLatestOpenJobs(
                JobStatus.OPEN.getValue(), PageRequest.of(0, RecommendationConstant.MAX_CANDIDATES));
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        PriorityQueue<ScoredJob> pq = new PriorityQueue<>(limit);

        for (Job job : candidates) {
            if (appliedJobIds.contains(job.getId())) {
                continue;
            }

            double score = calculateScore(
                    job, userSkillNames, userAddress, appliedTypes, recentJobIds, trendingJobIds);

            if (score > 0) {
                pq.offer(new ScoredJob(job.getId(), score));
                if (pq.size() > limit) {
                    pq.poll();
                }
            }
        }

        List<UUID> result = new ArrayList<>(pq.size());
        while (!pq.isEmpty()) {
            result.add(pq.poll().jobId);
        }
        Collections.reverse(result);
        return result;
    }

    private Set<String> collectUserSkillNames(User user) {
        if (user.getSkills() == null) return Collections.emptySet();
        return user.getSkills().stream()
                .map(Skill::getName)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<String> collectAppliedJobTypes(List<Application> applications) {
        return applications.stream()
                .map(a -> a.getJob().getType())
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<UUID> collectAppliedJobIds(List<Application> applications) {
        return applications.stream()
                .map(a -> a.getJob().getId())
                .collect(Collectors.toSet());
    }

    private Set<UUID> collectRecentJobIds(UUID userId) {
        return new HashSet<>(recentViewService.getRecentViewIds(
                userId, RecommendationConstant.RECENT_JOB_LIMIT));
    }

    private Set<UUID> collectTrendingJobIds() {
        return new HashSet<>(trendingJobService.getTopJobIds(
                RecommendationConstant.TRENDING_JOB_LIMIT));
    }

    private double calculateScore(Job job, Set<String> userSkillNames,
                                  String userAddress, Set<String> appliedTypes,
                                  Set<UUID> recentJobIds, Set<UUID> trendingJobIds) {
        double score = 0;

        score += calculateSkillScore(job, userSkillNames);

        if (isLocationMatch(job, userAddress)) {
            score += RecommendationConstant.SCORE_LOCATION_MATCH;
        }

        if (job.getType() != null && appliedTypes.contains(job.getType().toLowerCase(Locale.ROOT))) {
            score += RecommendationConstant.SCORE_APPLIED_SAME_TYPE;
        }

        if (recentJobIds.contains(job.getId())) {
            score += RecommendationConstant.SCORE_RECENTLY_VIEWED;
        }

        if (trendingJobIds.contains(job.getId())) {
            score += RecommendationConstant.SCORE_TRENDING;
        }

        return score;
    }

    private double calculateSkillScore(Job job, Set<String> userSkillNames) {
        if (userSkillNames.isEmpty()) return 0;

        Set<String> jobSkillNames = job.getSkills() != null
                ? job.getSkills().stream().map(Skill::getName)
                .map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet())
                : Collections.emptySet();

        if (jobSkillNames.isEmpty()) return 0;

        long matched = userSkillNames.stream().filter(jobSkillNames::contains).count();
        return matched * (double)RecommendationConstant.SCORE_SKILL_MATCH;
    }

    private boolean isLocationMatch(Job job, String userAddress) {
        if (userAddress.isEmpty()) return false;
        String jobLocation = job.getWorkLocation() != null ? job.getWorkLocation().toLowerCase(Locale.ROOT) : "";
        if (jobLocation.isEmpty()) return false;
        Set<String> userWords = new HashSet<>(Arrays.asList(userAddress.split("\\s+")));
        Set<String> jobWords = new HashSet<>(Arrays.asList(jobLocation.split("\\s+")));
        userWords.retainAll(jobWords);
        return !userWords.isEmpty();
    }

    private void cacheRecommendations(UUID userId, List<UUID> jobIds) {
        RedisOperation.run(() -> {
            String key = RedisKeys.recommendKey(userId);
            stringRedisTemplate.delete(key);
            if (!jobIds.isEmpty()) {
                String[] members = jobIds.stream().map(UUID::toString).toArray(String[]::new);
                stringRedisTemplate.opsForList().rightPushAll(key, members);
                stringRedisTemplate.expire(key, RecommendationConstant.CACHE_TTL);
            }
        }, "Failed to cache recommendations for user {}", userId);
    }

    private record ScoredJob(UUID jobId, double score) implements Comparable<ScoredJob> {
        @Override
        public int compareTo(ScoredJob o) {
            return Double.compare(this.score, o.score);
        }
    }
}
