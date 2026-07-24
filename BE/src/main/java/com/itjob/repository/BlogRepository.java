package com.itjob.repository;

import com.itjob.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogRepository extends JpaRepository<Blog, UUID>, JpaSpecificationExecutor<Blog> {
    
    // Find by ID (excluding deleted) with User and Category eagerly loaded
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT b FROM Blog b WHERE b.id = :id AND (b.isDeleted IS NULL OR b.isDeleted = false)")
    Optional<Blog> findActiveById(@Param("id") UUID id);
    
    // Find by category (excluding deleted) with User and Category eagerly loaded
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT b FROM Blog b WHERE b.category.id = :categoryId AND (b.isDeleted IS NULL OR b.isDeleted = false)")
    Page<Blog> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);
    
    // Find by user (excluding deleted)
    @Query("SELECT b FROM Blog b WHERE b.user.id = :userId AND (b.isDeleted IS NULL OR b.isDeleted = false)")
    Page<Blog> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    // Fetch recent blogs (excluding deleted) with User and Category eagerly loaded
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT b FROM Blog b WHERE (b.isDeleted IS NULL OR b.isDeleted = false) ORDER BY b.createdAt DESC")
    List<Blog> findRecentBlogs(Pageable pageable);
    
    // Find by slug
    Optional<Blog> findBySlug(String slug);

    // Batch increment view count (used by ViewCountService sync)
    @Modifying
    @Transactional
    @Query("UPDATE Blog b SET b.viewCount = COALESCE(b.viewCount, 0) + :count WHERE b.id = :id")
    int incrementViewCount(@Param("id") UUID id, @Param("count") long count);
}
