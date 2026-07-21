package com.itjob.service.impl;

import com.itjob.redis.CacheName;
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
import com.itjob.enums.ViewEntity;
import com.itjob.service.BlogService;
import com.itjob.service.ViewCountService;
import com.itjob.specification.helper.SpecificationHelper;
import com.itjob.util.PageResponseUtil;
import com.itjob.util.SlugUtil;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final ViewCountService viewCountService;

    @Override
    @Cacheable(value = CacheName.BLOG_RECENT,
               key = "T(com.itjob.util.CacheKeyGenerator).forLimit(#limit)")
    public List<BlogBriefResponse> getRecentBlogs(int limit) {
        if (limit <= 0) {
            throw new AppException(ErrorCode.INVALID_LIMIT);
        }

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
                     "'filters', T(com.itjob.util.CacheKeyGenerator).forFilters(#filters), " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize, " +
                     "'sort', #pageable.sort.isSorted() ? #pageable.sort.toString() : ''))")
    public PageResponse<BlogBriefResponse> getAllBlogs(String[] filters, Pageable pageable) {
        long start = System.currentTimeMillis();
        log.debug("Fetching all blogs with filters from database");

        Specification<Blog> spec = specificationHelper.buildSpecification(filters);
        Specification<Blog> notDeleted = (root, query, cb) ->
                cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted")));
        spec = notDeleted.and(spec);
        Page<Blog> blogPage = blogRepository.findAll(spec, pageable);

        PageResponse<BlogBriefResponse> result = buildBlogPageResponse(blogPage);
        log.debug("getAllBlogs completed in {} ms", System.currentTimeMillis() - start);
        return result;
    }

    @Override
    @Cacheable(value = CacheName.BLOG_BY_CATEGORY,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'categoryId', #categoryId, " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize, " +
                     "'sort', #pageable.sort.isSorted() ? #pageable.sort.toString() : ''))")
    public PageResponse<BlogBriefResponse> getBlogsByCategory(UUID categoryId, Pageable pageable) {
        long start = System.currentTimeMillis();
        log.debug("Fetching blogs by category: {} from database", categoryId);

        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));

        Page<Blog> blogPage = blogRepository.findByCategoryId(categoryId, pageable);

        PageResponse<BlogBriefResponse> result = buildBlogPageResponse(blogPage);
        log.debug("getBlogsByCategory({}) completed in {} ms", categoryId, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public BlogResponse getBlogById(UUID id) {
        BlogResponse response = blogCacheService.getCachedBlogById(id);

        viewCountService.incrementView(ViewEntity.BLOG, id);

        long pendingViews = viewCountService.getPendingViewDelta(ViewEntity.BLOG, id);
        if (pendingViews > 0) {
            Integer current = response.getViewCount();
            response.setViewCount((current == null ? 0 : current) + (int) pendingViews);
        }

        return response;
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageResponse<BlogBriefResponse> getMyBlogs(UUID userId, Pageable pageable) {
        log.debug("Getting blogs for user: {}", userId);

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
        log.debug("Creating blog for user: {}", userId);

        User user = userRepository.getReferenceById(userId);

        BlogCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));

        Blog blog = blogMapper.toBlog(request);
        blog.setUser(user);
        blog.setCategory(category);
        blog.setSlug(generateUniqueBlogSlug(request.getTitle()));

        blog = blogRepository.save(blog);

        log.debug("Blog created successfully with id: {}", blog.getId());

        return blogMapper.toBlogResponse(blog);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.BLOG_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.BLOG_RECENT, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_BY_CATEGORY, allEntries = true)
    })
    public BlogResponse updateBlog(UUID id, UUID userId, BlogRequest request) {
        log.debug("Updating blog {} by user {}", id, userId);

        Blog blog = blogRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        verifyBlogOwnership(blog, userId);

        if (!Objects.equals(blog.getCategory().getId(), request.getCategoryId())) {
            BlogCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));
            blog.setCategory(category);
        }

        String oldTitle = blog.getTitle();

        blogMapper.updateBlog(blog, request);

        if (request.getTitle() != null && !request.getTitle().equals(oldTitle)) {
            blog.setSlug(generateUniqueBlogSlug(request.getTitle(), id));
        }

        blog = blogRepository.save(blog);

        log.debug("Blog updated successfully");

        return blogMapper.toBlogResponse(blog);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.BLOG_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.BLOG_RECENT, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.BLOG_BY_CATEGORY, allEntries = true)
    })
    public void deleteBlog(UUID id, UUID userId) {
        log.debug("Deleting blog {} by user {}", id, userId);

        Blog blog = blogRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        verifyBlogOwnership(blog, userId);

        blog.setIsDeleted(true);
        blog.setDeletedAt(LocalDateTime.now());
        blogRepository.save(blog);

        log.debug("Blog deleted successfully");
    }

    private void verifyBlogOwnership(Blog blog, UUID userId) {
        if (!blog.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private String generateUniqueBlogSlug(String title) {
        return generateUniqueBlogSlug(title, null);
    }

    private String generateUniqueBlogSlug(String title, UUID excludeId) {
        String baseSlug = SlugUtil.generateSlug(title);
        String slug = baseSlug;
        int counter = 1;
        Optional<Blog> existing = blogRepository.findBySlug(slug);
        while (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            slug = baseSlug + "-" + counter++;
            existing = blogRepository.findBySlug(slug);
        }
        return slug;
    }

    private PageResponse<BlogBriefResponse> buildBlogPageResponse(Page<Blog> blogPage) {
        return PageResponseUtil.build(blogPage, blogMapper::toBlogBriefResponse);
    }
}
