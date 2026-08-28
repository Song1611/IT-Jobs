package com.itjob.integration.service;

import com.itjob.dto.response.CommentResponse;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.PostRepository;
import com.itjob.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - CommentService")
class CommentServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("create -> adds a comment to a post")
    void createAddsComment() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Post").build());
        User commenter = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");

        CommentResponse response = commentService.create(post.getId(), commenter.getId(), "Nice post!");

        assertThat(response.getContent()).isEqualTo("Nice post!");
        assertThat(response.getUser().getEmail()).isEqualTo(commenter.getEmail());
    }

    @Test
    @DisplayName("create -> throws COMMENT_CONTENT_REQUIRED for blank content")
    void createBlankContentThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Post").build());

        UUID postId = post.getId();
        UUID userId = author.getId();
        assertThatThrownBy(() -> commentService.create(postId, userId, "   "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_CONTENT_REQUIRED);
    }

    @Test
    @DisplayName("create -> throws POST_NOT_FOUND for a missing post")
    void createMissingPostThrows() {
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");

        UUID randomPostId = UUID.randomUUID();
        UUID userId = user.getId();
        assertThatThrownBy(() -> commentService.create(randomPostId, userId, "Hello"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("delete -> comment author can delete their own comment")
    void deleteByAuthor() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Post").build());
        User commenter = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        CommentResponse created = commentService.create(post.getId(), commenter.getId(), "My comment");

        commentService.delete(post.getId(), created.getId(), commenter.getId());

        var page = commentService.getByPost(post.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("delete -> throws FORBIDDEN for a user who is neither author nor post author")
    void deleteByOtherUserThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Post").build());
        User commenter = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        CommentResponse created = commentService.create(post.getId(), commenter.getId(), "My comment");

        User stranger = createVerifiedUser("stranger-" + UUID.randomUUID() + "@example.com");

        UUID postId = post.getId();
        UUID commentId = created.getId();
        UUID strangerId = stranger.getId();
        assertThatThrownBy(() -> commentService.delete(postId, commentId, strangerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("getByPost -> returns comments of a post")
    void getByPostReturnsComments() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Post").build());
        User commenter = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        commentService.create(post.getId(), commenter.getId(), "First");
        commentService.create(post.getId(), commenter.getId(), "Second");

        var page = commentService.getByPost(post.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}