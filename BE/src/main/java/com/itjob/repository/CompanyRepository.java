package com.itjob.repository;

import com.itjob.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    // Find top companies (by follower count or job count)
    @Query("SELECT c FROM Company c WHERE c.status = :status AND c.isDeleted = false " +
           "ORDER BY c.followerCount DESC, c.viewCount DESC")
    List<Company> findTopCompanies(@Param("status") String status, Pageable pageable);
    
    // Find companies by status
    Page<Company> findByStatusAndIsDeleted(String status, Boolean isDeleted, Pageable pageable);
    
    // Find all non-deleted companies
    Page<Company> findByIsDeleted(Boolean isDeleted, Pageable pageable);
    
    // Find active companies
    @Query("SELECT c FROM Company c WHERE c.status = :status AND c.isDeleted = false")
    Page<Company> findActiveCompanies(@Param("status") String status, Pageable pageable);
    
    // Find company by slug
    Optional<Company> findBySlugAndIsDeleted(String slug, Boolean isDeleted);

    // Find company by slug (regardless of isDeleted) for duplicate check
    Optional<Company> findBySlug(String slug);
    
    // Find company by ID (excluding deleted)
    Optional<Company> findByIdAndIsDeleted(UUID id, Boolean isDeleted);
    
    // Find company by creator user ID (excluding deleted)
    Optional<Company> findByCreatedByIdAndIsDeleted(UUID userId, Boolean isDeleted);
    
    // Check if user already has a company (excluding deleted)
    boolean existsByCreatedByIdAndIsDeleted(UUID userId, Boolean isDeleted);
    
    // Count companies by status
    long countByStatus(String status);
    
    // Count active companies
    long countByStatusAndIsDeleted(String status, Boolean isDeleted);

    // Batch increment view count (used by ViewCountService sync)
    @Modifying
    @Query("UPDATE Company c SET c.viewCount = COALESCE(c.viewCount, 0) + :count WHERE c.id = :id")
    int incrementViewCount(@Param("id") UUID id, @Param("count") long count);
}
