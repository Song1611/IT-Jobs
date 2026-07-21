package com.itjob.repository;

import com.itjob.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, String status, Pageable pageable);

    Page<Review> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Review> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
}
