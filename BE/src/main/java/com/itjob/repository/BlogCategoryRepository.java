package com.itjob.repository;

import com.itjob.entity.BlogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, UUID> {
    
    Optional<BlogCategory> findByName(String name);
    
    boolean existsByName(String name);
}
