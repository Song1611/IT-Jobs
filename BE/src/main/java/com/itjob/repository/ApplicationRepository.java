package com.itjob.repository;

import com.itjob.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    
    // Check if user already applied to a job
    boolean existsByJobIdAndUserId(UUID jobId, UUID userId);
    
    // Find application by job and user
    Optional<Application> findByJobIdAndUserId(UUID jobId, UUID userId);
    
    // Find applications by user (for candidate view)
    Page<Application> findByUserIdOrderByAppliedAtDesc(UUID userId, Pageable pageable);
    
    // Find applications by job (for HR view)
    Page<Application> findByJobIdOrderByAppliedAtDesc(UUID jobId, Pageable pageable);
    
    // Find applications by job and status
    Page<Application> findByJobIdAndStatusOrderByAppliedAtDesc(UUID jobId, String status, Pageable pageable);
    
    // Find applications by company (through job)
    @Query("SELECT a FROM Application a WHERE a.job.company.id = :companyId ORDER BY a.appliedAt DESC")
    Page<Application> findByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);
    
    // Count new applications for a company
    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.company.id = :companyId " +
           "AND a.status = 'pending' AND a.appliedAt > :since")
    long countNewApplicationsByCompanyId(@Param("companyId") UUID companyId, @Param("since") LocalDateTime since);
    
    // Count applications by job
    long countByJobId(UUID jobId);
    
    // Count applications by user
    long countByUserId(UUID userId);
}
