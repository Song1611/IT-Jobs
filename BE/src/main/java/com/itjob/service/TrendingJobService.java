package com.itjob.service;

import java.util.List;
import java.util.UUID;

public interface TrendingJobService {

    void recordScore(UUID jobId, double score);

    List<UUID> getTopJobIds(int limit);

    void removeJob(UUID jobId);
}
