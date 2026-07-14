package com.itjob.service.impl;

import com.itjob.constant.CacheName;
import com.itjob.dto.response.JobResponse;
import com.itjob.entity.Job;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.JobMapper;
import com.itjob.repository.JobRepository;
import com.itjob.service.JobCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Separated cache service to avoid Spring Cache proxy issues.
 * When @Cacheable methods are called from within the same class, Spring's proxy mechanism is bypassed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobCacheServiceImpl implements JobCacheService {
    
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    
    @Override
    @Cacheable(value = CacheName.JOB_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    public JobResponse getCachedJobById(UUID id) {
        log.debug("Fetching job {} from database", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        // Use mapper to convert Job to JobResponse with Company and Skills
        return jobMapper.toJobResponse(job);
    }
}
