package com.itjob.repository;

import com.itjob.entity.Job;
import com.itjob.repository.projection.CompanyJobCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    
    // Find by ID with Company and Skills eagerly loaded (to avoid N+1)
    @EntityGraph(attributePaths = {"company", "skills"})
    Optional<Job> findById(UUID id);
    
    // Find featured/latest jobs with Company and Skills eagerly loaded
    @EntityGraph(attributePaths = {"company", "skills"})
    @Query("SELECT j FROM Job j WHERE j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findFeaturedJobs(@Param("status") String status, Pageable pageable);
    
    // Find jobs by company
    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    // Check if slug already exists
    Optional<Job> findBySlug(String slug);
    
    // Find jobs by company and status
    Page<Job> findByCompanyIdAndStatus(UUID companyId, String status, Pageable pageable);
    
    // Find jobs by status
    Page<Job> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "skills"})
    @Query("SELECT j FROM Job j WHERE j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findLatestOpenJobs(@Param("status") String status, Pageable pageable);
    
    // Count jobs by company
    long countByCompanyId(UUID companyId);
    
    // Count active jobs by company
    long countByCompanyIdAndStatus(UUID companyId, String status);
    
    // Batch query: Get job counts for multiple companies (to avoid N+1)
    // Using projection interface for type-safe results
    @Query("SELECT j.company.id AS companyId, COUNT(j) AS jobCount FROM Job j " +
           "WHERE j.company.id IN :companyIds AND j.status = :status " +
           "GROUP BY j.company.id")
    List<CompanyJobCountProjection> countJobsByCompanyIdsAndStatus(List<UUID> companyIds, String status);

    // Batch increment view count (used by ViewCountService sync)
    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.viewCount = COALESCE(j.viewCount, 0) + :count WHERE j.id = :id")
    int incrementViewCount(@Param("id") UUID id, @Param("count") long count);
}
