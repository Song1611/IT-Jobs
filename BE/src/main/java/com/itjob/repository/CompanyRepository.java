package com.itjob.repository;

import com.itjob.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    // Find top companies (by follower count or job count)
    @Query("SELECT c FROM Company c WHERE c.status = 'active' AND c.isDeleted = false " +
           "ORDER BY c.followerCount DESC, c.viewCount DESC")
    List<Company> findTopCompanies(Pageable pageable);
    
    // Find companies by status
    Page<Company> findByStatusAndIsDeleted(String status, Boolean isDeleted, Pageable pageable);
    
    // Find all non-deleted companies
    Page<Company> findByIsDeleted(Boolean isDeleted, Pageable pageable);
    
    // Find active companies
    @Query("SELECT c FROM Company c WHERE c.status = 'active' AND c.isDeleted = false")
    Page<Company> findActiveCompanies(Pageable pageable);
    
    // Find company by slug
    Optional<Company> findBySlugAndIsDeleted(String slug, Boolean isDeleted);
    
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
}
