package com.itjob.repository;

import com.itjob.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogRepository extends JpaRepository<Blog, UUID>, JpaSpecificationExecutor<Blog> {
    
    // Find by ID with User and Category eagerly loaded
    @EntityGraph(attributePaths = {"user", "category"})
    Optional<Blog> findById(UUID id);
    
    // Find by category with User and Category eagerly loaded
    @EntityGraph(attributePaths = {"user", "category"})
    Page<Blog> findByCategoryId(UUID categoryId, Pageable pageable);
    
    Page<Blog> findByUserId(UUID userId, Pageable pageable);
    
    // Fetch recent blogs with User and Category eagerly loaded
    @EntityGraph(attributePaths = {"user", "category"})
    @Query("SELECT b FROM Blog b ORDER BY b.createdAt DESC")
    List<Blog> findRecentBlogs(Pageable pageable);
}
