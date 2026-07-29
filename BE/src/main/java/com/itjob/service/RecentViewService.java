package com.itjob.service;

import java.util.List;
import java.util.UUID;

public interface RecentViewService {

    void recordView(UUID userId, UUID jobId);

    List<UUID> getRecentViewIds(UUID userId, int limit);
}
