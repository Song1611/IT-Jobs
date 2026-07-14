package com.itjob.repository.projection;

import java.util.UUID;

/**
 * Projection interface for batch querying company job counts
 * Type-safe alternative to Object[] for query results
 */
public interface CompanyJobCountProjection {
    UUID getCompanyId();
    Long getJobCount();
}
