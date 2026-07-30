package com.itjob.service;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {

    List<UUID> getRecommendedJobs(UUID userId, int limit);

    void invalidateCache(UUID userId);
}
