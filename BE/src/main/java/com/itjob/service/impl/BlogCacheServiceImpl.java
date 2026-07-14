package com.itjob.service.impl;

import com.itjob.constant.CacheName;
import com.itjob.dto.response.BlogResponse;
import com.itjob.entity.Blog;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.BlogMapper;
import com.itjob.repository.BlogRepository;
import com.itjob.service.BlogCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogCacheServiceImpl implements BlogCacheService {
    
    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    
    @Override
    @Cacheable(value = CacheName.BLOG_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    public BlogResponse getCachedBlogById(UUID id) {
        log.debug("Fetching blog {} from database", id);
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        return blogMapper.toBlogResponse(blog);
    }
}
