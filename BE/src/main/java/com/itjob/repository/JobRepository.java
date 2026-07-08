package com.itjob.repository;

import com.itjob.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    
    // Find featured/latest jobs
    @Query("SELECT j FROM Job j WHERE j.status = 'open' ORDER BY j.createdAt DESC")
    List<Job> findFeaturedJobs(Pageable pageable);
    
    // Find jobs by company
    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);
    
    // Find jobs by company and status
    Page<Job> findByCompanyIdAndStatus(UUID companyId, String status, Pageable pageable);
    
    // Count jobs by company
    long countByCompanyId(UUID companyId);
    
    // Count active jobs by company
    long countByCompanyIdAndStatus(UUID companyId, String status);
}
