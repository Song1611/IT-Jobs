package com.itjob.service;

import com.itjob.enums.ViewEntity;

import java.util.UUID;

public interface ViewCountService {

    void incrementView(ViewEntity entity, UUID id);

    void incrementView(ViewEntity entity, UUID id, String viewerId);

    long getPendingViewDelta(ViewEntity entity, UUID id);
}
