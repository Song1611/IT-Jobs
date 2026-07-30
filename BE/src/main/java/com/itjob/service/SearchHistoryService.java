package com.itjob.service;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryService {

    void recordSearch(UUID userId, String keyword);

    List<String> getSearchHistory(UUID userId, int limit);
}
