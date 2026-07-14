package com.itjob.service.impl;

import com.itjob.constant.CacheName;
import com.itjob.dto.request.BlogRequest;
import com.itjob.dto.response.BlogBriefResponse;
import com.itjob.dto.response.BlogResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Blog;
import com.itjob.entity.BlogCategory;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.BlogMapper;
import com.itjob.repository.BlogCategoryRepository;
import com.itjob.repository.BlogRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.BlogCacheService;
import com.itjob.service.BlogService;
import com.itjob.specification.helper.SpecificationHelper;
import com.itjob.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {
    
    private static final int MAX_RECENT_BLOGS = 100;
    
    private final BlogRepository blogRepository;
    private final BlogCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BlogMapper blogMapper;
    private final SpecificationHelper specificationHelper;
    private final BlogCacheService blogCacheService;
    
    @Override
    @Cacheable(value = CacheName.BLOG_RECENT, 
               key = "T(com.itjob.util.CacheKeyGenerator).forLimit(#limit)")
    public List<BlogBriefResponse> getRecentBlogs(int limit) {
        if (limit <= 0) {
            throw new AppException(ErrorCode.INVALID_LIMIT);
        }
        
        // Reject if limit exceeds maximum
        if (limit > MAX_RECENT_BLOGS) {
            throw new AppException(ErrorCode.LIMIT_EXCEEDED);
        }
        
        log.debug("Fetching {} recent blogs from database", limit);
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Blog> blogs = blogRepository.findRecentBlogs(pageable);
        
        return blogs.stream()
                .map(blogMapper::toBlogBriefResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = CacheName.BLOG_SEARCH,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'filters', #filters != null ? T(java.util.Arrays).toString(#filters) : 'none', " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize))")
    public PageResponse<BlogBriefResponse> getAllBlogs(String[] filters, Pageable pageable) {
        log.debug("Fetching all blogs with filters from database");
        
        Specification<Blog> spec = specificationHelper.buildSpecification(filters);
        Page<Blog> blogPage = blogRepository.findAll(spec, pageable);
        
        return buildBlogPageResponse(blogPage);
    }
    
    @Override
    @Cacheable(value = CacheName.BLOG_BY_CATEGORY,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'categoryId', #categoryId, " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize))")
    public PageResponse<BlogBriefResponse> getBlogsByCategory(UUID categoryId, Pageable pageable) {
        log.debug("Fetching blogs by category: {} from database", categoryId);
        
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));
        
        Page<Blog> blogPage = blogRepository.findByCategoryId(categoryId, pageable);
        
        return buildBlogPageResponse(blogPage);
    }
    
    @Override
    public BlogResponse getBlogById(UUID id) {
        // Get base blog data from cache (using separate service to avoid proxy bypass)

        // TODO: Implement view count tracking with RedisTemplate + async + scheduled DB sync
        
        return blogCacheService.getCachedBlogById(id);
    }
    
    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageResponse<BlogBriefResponse> getMyBlogs(UUID userId, Pageable pageable) {
        log.debug("Getting blogs for user: {}", userId);
        
        // NOTE: User-specific data, should NOT be cached
        // Or cache per user with user-specific key
        Page<Blog> blogPage = blogRepository.findByUserId(userId, pageable);
        
        return buildBlogPageResponse(blogPage);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.BLOG_RECENT, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_BY_CATEGORY, allEntries = true)
    })
    public BlogResponse createBlog(UUID userId, BlogRequest request) {
        log.info("Creating blog for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        BlogCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));
        
        Blog blog = blogMapper.toBlog(request);
        blog.setUser(user);
        blog.setCategory(category);
        
        blog = blogRepository.save(blog);
        
        log.info("Blog created successfully with id: {}", blog.getId());
        
        // Use mapper
        return blogMapper.toBlogResponse(blog);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.BLOG_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.BLOG_RECENT, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_BY_CATEGORY, allEntries = true)
    })
    public BlogResponse updateBlog(UUID id, UUID userId, BlogRequest request) {
        log.info("Updating blog {} by user {}", id, userId);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        // Only author can update (removed ADMIN from annotation)
        verifyBlogOwnership(blog, userId);
        
        // Update category if changed (use Objects.equals to handle null safely)
        if (!Objects.equals(blog.getCategory().getId(), request.getCategoryId())) {
            BlogCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));
            blog.setCategory(category);
        }
        
        blogMapper.updateBlog(blog, request);
        blog = blogRepository.save(blog);
        
        log.info("Blog updated successfully");
        
        // Use mapper
        return blogMapper.toBlogResponse(blog);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.BLOG_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.BLOG_RECENT, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_BY_CATEGORY, allEntries = true)
    })
    public void deleteBlog(UUID id, UUID userId) {
        log.info("Deleting blog {} by user {}", id, userId);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        // Only author can delete (removed ADMIN from annotation)
        verifyBlogOwnership(blog, userId);
        
        blogRepository.delete(blog);
        
        log.info("Blog deleted successfully");
    }
    
    /**
     * Verify that the user owns the blog
     */
    private void verifyBlogOwnership(Blog blog, UUID userId) {
        if (!blog.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    // Helper method to build page response
    private PageResponse<BlogBriefResponse> buildBlogPageResponse(Page<Blog> blogPage) {
        return PageResponseUtil.build(blogPage, blogMapper::toBlogBriefResponse);
    }
}
