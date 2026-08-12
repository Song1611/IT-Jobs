package com.itjob.service.impl;

import com.itjob.dto.response.CommentResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Comment;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.CommentMapper;
import com.itjob.repository.CommentRepository;
import com.itjob.repository.PostRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResponse create(UUID postId, UUID userId, String content) {
        requireContent(content);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build();
        comment = commentRepository.save(comment);

        // Comment count is a plain DB counter (not cached), updated in the same
        // transaction as the insert, so PostResponse.totalComments stays consistent.
        postRepository.incrementCommentCount(postId, 1);
        log.info("Created comment {} on post {} by user {}", comment.getId(), postId, userId);
        return commentMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional
    public void delete(UUID postId, UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }

        boolean isCommentAuthor = comment.getAuthor().getId().equals(userId);
        boolean isPostAuthor = comment.getPost().getAuthor().getId().equals(userId);
        if (!isCommentAuthor && !isPostAuthor) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        commentRepository.delete(comment);
        postRepository.incrementCommentCount(postId, -1);
        log.info("Deleted comment {} on post {} by user {}", commentId, postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getByPost(UUID postId, Pageable pageable) {
        Page<Comment> page = commentRepository.findByPostId(postId, pageable);

        List<CommentResponse> items = page.getContent().stream()
                .map(commentMapper::toCommentResponse)
                .toList();

        return PageResponse.<CommentResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private void requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.COMMENT_CONTENT_REQUIRED);
        }
    }
}
