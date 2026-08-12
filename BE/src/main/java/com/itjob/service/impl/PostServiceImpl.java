package com.itjob.service.impl;

import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.PostInteractionResponse;
import com.itjob.dto.response.PostResponse;
import com.itjob.entity.Attachment;
import com.itjob.entity.Company;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.enums.ReactionEntity;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.PostMapper;
import com.itjob.repository.AttachmentRepository;
import com.itjob.repository.CompanyMemberRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.PostRepository;
import com.itjob.repository.ReactionRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.PostCacheService;
import com.itjob.service.PostService;
import com.itjob.service.ReactionService;
import com.itjob.service.storage.CloudinaryService;
import com.itjob.service.storage.CloudinaryUploadResult;
import com.itjob.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final ReactionRepository reactionRepository;
    private final ReactionService reactionService;
    private final PostMapper postMapper;
    private final CloudinaryService cloudinaryService;
    private final PostCacheService postCacheService;

    private static final String UPLOAD_FOLDER = "itjob/posts";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getAll(Pageable pageable) {
        PageResponse<PostResponse> cached = postCacheService.getCachedPostList(pageable);
        return enrichPage(cached);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getById(UUID id) {
        PostResponse cached = postCacheService.getCachedPostDetail(id);
        return enrichInteraction(cached);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getByUser(UUID userId, Pageable pageable) {
        PageResponse<PostResponse> cached = postCacheService.getCachedPostsByUser(userId, pageable);
        return enrichPage(cached);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getByCompany(UUID companyId, Pageable pageable) {
        PageResponse<PostResponse> cached = postCacheService.getCachedPostsByCompany(companyId, pageable);
        return enrichPage(cached);
    }

    @Override
    @Transactional
    public PostResponse create(UUID userId, String content, UUID companyId,
                               List<MultipartFile> images, MultipartFile video) {
        requireContent(content);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Company company = null;
        if (companyId != null) {
            company = validateCompanyAccess(userId, companyId);
        }

        Post post = Post.builder()
                .author(author)
                .company(company)
                .content(content)
                .build();
        post = postRepository.save(post);

        saveAttachments(post, images, video);

        evictCacheAfterCommit(postCacheService::evictAll);
        log.info("Created post {} by user {}", post.getId(), userId);
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse update(UUID postId, UUID userId, String content) {
        Post post = getOwnedPost(postId, userId);
        requireContent(content);
        post.setContent(content);
        post = postRepository.save(post);
        evictCacheAfterCommit(() -> {
            postCacheService.evictPost(postId);
            postCacheService.evictAll();
        });
        log.info("Updated post content, postId={}, userId={}", postId, userId);
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse updateWithImages(UUID postId, UUID userId, String content,
                                         List<MultipartFile> images) {
        Post post = getOwnedPost(postId, userId);
        requireContent(content);
        post.setContent(content);
        post = postRepository.save(post);
        int addedImages = saveAttachments(post, images, null);
        evictCacheAfterCommit(() -> {
            postCacheService.evictPost(postId);
            postCacheService.evictAll();
        });
        log.info("Updated post with images, postId={}, userId={}, addedImages={}", postId, userId, addedImages);
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public void delete(UUID postId, UUID userId) {
        Post post = getOwnedPost(postId, userId);
        List<Attachment> attachments = attachmentRepository.findByPostId(postId);

        postRepository.delete(post);

        // After the DB transaction commits successfully: delete files on Cloudinary (best-effort)
        // and evict cache, so a failed transaction causes a cache miss instead of stale data.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteCloudinaryFiles(attachments);
                postCacheService.evictPost(postId);
                postCacheService.evictAll();
            }
        });
        log.info("Deleted post {} by user {}", postId, userId);
    }

    /** Evict cache only after the transaction commits; if no transaction is active, evict immediately. */
    private void evictCacheAfterCommit(Runnable eviction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }

    private Post getOwnedPost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return post;
    }

    private void requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.POST_CONTENT_REQUIRED);
        }
    }

    private Company validateCompanyAccess(UUID userId, UUID companyId) {
        Company company = companyRepository.findByIdAndIsDeleted(companyId, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        boolean isCreator = company.getCreatedBy() != null
                && company.getCreatedBy().getId().equals(userId);
        boolean isActiveMember = companyMemberRepository.existsByCompanyIdAndUserIdAndStatus(
                companyId, userId, "active");

        if (!isCreator && !isActiveMember) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        return company;
    }

    private int saveAttachments(Post post, List<MultipartFile> images, MultipartFile video) {
        List<Attachment> attachments = new ArrayList<>();

        if (images != null) {
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) continue;
                CloudinaryUploadResult result = cloudinaryService.upload(image, UPLOAD_FOLDER);
                attachments.add(Attachment.builder()
                        .post(post)
                        .fileType("image")
                        .fileUrl(result.url())
                        .filePublicId(result.publicId())
                        .build());
            }
        }

        if (video != null && !video.isEmpty()) {
            CloudinaryUploadResult result = cloudinaryService.upload(video, UPLOAD_FOLDER);
            attachments.add(Attachment.builder()
                    .post(post)
                    .fileType("video")
                    .fileUrl(result.url())
                    .filePublicId(result.publicId())
                    .build());
        }

        if (!attachments.isEmpty()) {
            attachmentRepository.saveAll(attachments);
        }
        return attachments.size();
    }

    private void deleteCloudinaryFiles(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return;

        for (Attachment attachment : attachments) {
            if (attachment.getFilePublicId() == null) continue;
            try {
                cloudinaryService.delete(attachment.getFilePublicId(), attachment.getFileType());
            } catch (Exception e) {
                log.warn("Failed to delete Cloudinary file. publicId={}", attachment.getFilePublicId(), e);
            }
        }
    }

    private Map<UUID, List<Attachment>> loadAttachmentsByPost(List<UUID> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return attachmentRepository.findByPostIdIn(postIds).stream()
                .collect(Collectors.groupingBy(a -> a.getPost().getId()));
    }

    private Set<UUID> loadLikedPostIds(List<UUID> postIds) {
        if (postIds.isEmpty()) return Set.of();

        UUID currentUserId = resolveCurrentUserId();
        if (currentUserId == null) return Set.of();

        return reactionRepository.findByUserIdAndPostIdIn(currentUserId, postIds).stream()
                .map(r -> r.getPost().getId())
                .collect(Collectors.toSet());
    }

    private UUID resolveCurrentUserId() {
        if (!SecurityUtil.isAuthenticated()) return null;
        try {
            return SecurityUtil.getCurrentUserId();
        } catch (AppException e) {
            log.warn("Failed to resolve current user, treating as anonymous: {}", e.getMessage());
            return null;
        }
    }

    private PostResponse toPostResponse(Post post) {
        Map<UUID, List<Attachment>> attachmentsByPost = loadAttachmentsByPost(List.of(post.getId()));
        Set<UUID> likedPostIds = loadLikedPostIds(List.of(post.getId()));
        return toPostResponse(post,
                attachmentsByPost.getOrDefault(post.getId(), List.of()),
                likedPostIds.contains(post.getId()));
    }

    private PostResponse toPostResponse(Post post, List<Attachment> attachments, boolean likedByCurrentUser) {
        PostResponse response = postMapper.toPostResponse(post);

        response.setAttachments(attachments.stream()
                .map(postMapper::toAttachmentResponse)
                .toList());

        long totalLikes = (post.getReactionCount() == null ? 0 : post.getReactionCount())
                + reactionService.getPendingReactionDelta(ReactionEntity.POST, post.getId());

        response.setInteraction(PostInteractionResponse.builder()
                .totalLikes(totalLikes)
                .likedByCurrentUser(likedByCurrentUser)
                .totalComments(post.getCommentCount() == null ? 0 : post.getCommentCount())
                .build());

        return response;
    }

    /** Rehydrate dynamic interaction on a page loaded from the static cache. */
    private PageResponse<PostResponse> enrichPage(PageResponse<PostResponse> cachedPage) {
        List<PostResponse> items = cachedPage.getItems();
        if (items == null || items.isEmpty()) {
            return cachedPage;
        }

        List<UUID> postIds = items.stream().map(PostResponse::getId).toList();
        Set<UUID> likedPostIds = loadLikedPostIds(postIds);
        Map<UUID, long[]> countsByPost = loadInteractionCounts(postIds);

        return PageResponse.<PostResponse>builder()
                .items(items.stream()
                        .map(post -> rehydrateInteraction(post,
                                likedPostIds.contains(post.getId()),
                                countsByPost.getOrDefault(post.getId(), new long[0])))
                        .toList())
                .page(cachedPage.getPage())
                .size(cachedPage.getSize())
                .totalElements(cachedPage.getTotalElements())
                .totalPages(cachedPage.getTotalPages())
                .build();
    }

    /** Rebuild dynamic interaction on a single post loaded from the static cache. */
    private PostResponse enrichInteraction(PostResponse cachedPost) {
        boolean liked = loadLikedPostIds(List.of(cachedPost.getId())).contains(cachedPost.getId());
        long[] counts = loadInteractionCounts(List.of(cachedPost.getId()))
                .getOrDefault(cachedPost.getId(), new long[0]);
        return rehydrateInteraction(cachedPost, liked, counts);
    }

    /**
     * Load base interaction counters directly from DB, because the static cache
     * intentionally does not hold reaction/comment counts (they change too often
     * to be cached and invalidated for every like/comment).
     */
    private Map<UUID, long[]> loadInteractionCounts(List<UUID> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return postRepository.getInteractionCounts(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> new long[]{
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue()
                        }));
    }

    private PostResponse rehydrateInteraction(PostResponse post, boolean likedByCurrentUser, long[] counts) {
        PostResponse copy = copyStatic(post);

        long baseLikes = counts.length >= 1 ? counts[0] : 0;
        long baseComments = counts.length >= 2 ? counts[1] : 0;

        long totalLikes = baseLikes
                + reactionService.getPendingReactionDelta(ReactionEntity.POST, post.getId());

        copy.setInteraction(PostInteractionResponse.builder()
                .totalLikes(totalLikes)
                .likedByCurrentUser(likedByCurrentUser)
                .totalComments(baseComments)
                .build());

        return copy;
    }

    /**
     * Copy all static fields to a new response, so interaction can be attached to
     * the copy instead of mutating the object held in cache.
     */
    private PostResponse copyStatic(PostResponse source) {
        return PostResponse.builder()
                .id(source.getId())
                .user(source.getUser())
                .company(source.getCompany())
                .content(source.getContent())
                .attachments(source.getAttachments())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }
}
