package com.itjob.service.impl;

import com.itjob.dto.response.AttachmentResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.PostResponse;
import com.itjob.entity.Attachment;
import com.itjob.entity.Post;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.PostMapper;
import com.itjob.redis.CacheName;
import com.itjob.repository.AttachmentRepository;
import com.itjob.repository.PostRepository;
import com.itjob.service.PostCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caches only STATIC post data (content, author, company, attachments).
 * Interaction data (totalLikes, likedByCurrentUser, totalComments) is dynamic
 * and MUST be assembled per request with the current user, never cached.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheServiceImpl implements PostCacheService {

    private final PostRepository postRepository;
    private final AttachmentRepository attachmentRepository;
    private final PostMapper postMapper;

    @Override
    @Cacheable(value = CacheName.POST_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#postId)")
    public PostResponse getCachedPostDetail(UUID postId) {
        log.debug("Fetching post {} from database", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return buildStatic(post);
    }

    @Override
    @Cacheable(value = CacheName.POST_LIST, key = "T(com.itjob.util.CacheKeyGenerator).forPageable(#pageable)")
    public PageResponse<PostResponse> getCachedPostList(Pageable pageable) {
        log.debug("Fetching post list from database");
        Page<Post> page = postRepository.findAllTopLevel(pageable);
        return toStaticPage(page);
    }

    @Override
    @Cacheable(value = CacheName.POST_BY_COMPANY, key = "T(com.itjob.util.CacheKeyGenerator).forCompanyPage(#companyId, #pageable)")
    public PageResponse<PostResponse> getCachedPostsByCompany(UUID companyId, Pageable pageable) {
        log.debug("Fetching company {} posts from database", companyId);
        Page<Post> page = postRepository.findByCompanyIdTopLevel(companyId, pageable);
        return toStaticPage(page);
    }

    @Override
    @Cacheable(value = CacheName.POST_BY_USER, key = "T(com.itjob.util.CacheKeyGenerator).forUserPage(#userId, #pageable)")
    public PageResponse<PostResponse> getCachedPostsByUser(UUID userId, Pageable pageable) {
        log.debug("Fetching user {} posts from database", userId);
        Page<Post> page = postRepository.findByAuthorIdTopLevel(userId, pageable);
        return toStaticPage(page);
    }

    @Override
    @CacheEvict(value = CacheName.POST_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#postId)")
    public void evictPost(UUID postId) {
        log.debug("Evicted cached post {}", postId);
    }

    @Override
    @CacheEvict(value = {CacheName.POST_LIST, CacheName.POST_BY_COMPANY, CacheName.POST_BY_USER}, allEntries = true)
    public void evictAll() {
        log.debug("Evicted cached post lists");
    }

    private PostResponse buildStatic(Post post) {
        return buildStatic(post, attachmentRepository.findByPostId(post.getId()));
    }

    private PostResponse buildStatic(Post post, List<Attachment> attachments) {
        PostResponse response = postMapper.toPostResponse(post);

        List<AttachmentResponse> attachmentResponses = attachments.stream()
                .map(postMapper::toAttachmentResponse)
                .toList();

        response.setAttachments(attachmentResponses);

        // leave it null here so callers know it is not cached.
        response.setInteraction(null);

        return response;
    }

    private PageResponse<PostResponse> toStaticPage(Page<Post> page) {
        List<Post> posts = page.getContent();
        List<UUID> postIds = posts.stream().map(Post::getId).toList();

        Map<UUID, List<Attachment>> attachmentsByPost = postIds.isEmpty() ? Map.of()
                : attachmentRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(a -> a.getPost().getId()));

        List<PostResponse> items = posts.stream()
                .map(post -> buildStatic(post,
                        attachmentsByPost.getOrDefault(post.getId(), List.of())))
                .toList();

        return PageResponse.<PostResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}