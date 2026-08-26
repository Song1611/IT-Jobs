package com.itjob.integration.service;

import com.itjob.entity.Comment;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.enums.ReactionEntity;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.CommentRepository;
import com.itjob.repository.PostRepository;
import com.itjob.repository.ReactionRepository;
import com.itjob.service.impl.ReactionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - ReactionService")
class ReactionServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ReactionServiceImpl reactionService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Test
    @DisplayName("togglePostReaction -> creates a reaction on first toggle and persists it")
    void toggleCreatesReaction() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(user).content("Test post").build());

        var response = reactionService.togglePostReaction(post.getId(), user.getId(), "LIKE");

        assertThat(response.isReacted()).isTrue();
        assertThat(response.getReactionType()).isEqualTo("like");
        assertThat(response.getReactionCount()).isEqualTo(1);
        assertThat(reactionRepository.findByPostIdAndUserId(post.getId(), user.getId())).isPresent();
    }

    @Test
    @DisplayName("togglePostReaction -> removes the reaction when toggled again")
    void toggleRemovesReaction() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(user).content("Test post").build());
        reactionService.togglePostReaction(post.getId(), user.getId(), "like");

        var response = reactionService.togglePostReaction(post.getId(), user.getId(), "like");

        assertThat(response.isReacted()).isFalse();
        assertThat(response.getReactionCount()).isZero();
        assertThat(reactionRepository.findByPostIdAndUserId(post.getId(), user.getId())).isEmpty();
    }

    @Test
    @DisplayName("togglePostReaction -> increments count for different users")
    void toggleIncrementsCountForDifferentUsers() {
        User user1 = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        User user2 = createVerifiedUser("other-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(user1).content("Test post").build());

        reactionService.togglePostReaction(post.getId(), user1.getId(), "like");
        var response = reactionService.togglePostReaction(post.getId(), user2.getId(), "love");

        assertThat(response.getReactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("togglePostReaction -> changes reaction type without changing count")
    void toggleChangesType() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(user).content("Test post").build());
        reactionService.togglePostReaction(post.getId(), user.getId(), "like");

        var response = reactionService.togglePostReaction(post.getId(), user.getId(), "love");

        assertThat(response.isReacted()).isTrue();
        assertThat(response.getReactionType()).isEqualTo("love");
        assertThat(response.getReactionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("togglePostReaction -> throws REACTION_TYPE_REQUIRED for blank type")
    void toggleBlankTypeThrows() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(user).content("Test post").build());

        UUID postId = post.getId();
        UUID userId = user.getId();
        assertThatThrownBy(() -> reactionService.togglePostReaction(postId, userId, "  "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.REACTION_TYPE_REQUIRED);
    }

    @Test
    @DisplayName("togglePostReaction -> throws POST_NOT_FOUND for a missing post")
    void toggleMissingPostThrows() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID randomId = UUID.randomUUID();
        UUID userId = user.getId();
        assertThatThrownBy(() -> reactionService.togglePostReaction(randomId, userId, "like"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("togglePostReaction -> throws USER_NOT_FOUND for a missing user")
    void toggleMissingUserThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Test post").build());

        UUID postId = post.getId();
        UUID randomUserId = UUID.randomUUID();
        assertThatThrownBy(() -> reactionService.togglePostReaction(postId, randomUserId, "like"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("toggleCommentReaction -> creates then removes a comment reaction")
    void toggleCommentReactionCreatesAndRemoves() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Test post").build());
        Comment comment = commentRepository.save(
                Comment.builder().post(post).author(author).content("Test comment").build());

        var created = reactionService.toggleCommentReaction(comment.getId(), author.getId(), "like");
        assertThat(created.isReacted()).isTrue();
        assertThat(created.getReactionCount()).isEqualTo(1);

        var removed = reactionService.toggleCommentReaction(comment.getId(), author.getId(), "like");
        assertThat(removed.isReacted()).isFalse();
        assertThat(removed.getReactionCount()).isZero();
    }

    @Test
    @DisplayName("toggleCommentReaction -> throws COMMENT_NOT_FOUND for a missing comment")
    void toggleCommentMissingThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID randomCommentId = UUID.randomUUID();
        UUID userId = author.getId();
        assertThatThrownBy(() -> reactionService.toggleCommentReaction(randomCommentId, userId, "like"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("syncToDatabase -> clears the pending reaction delta")
    void syncToDatabaseClearsPendingDelta() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Test post").build());
        reactionService.togglePostReaction(post.getId(), author.getId(), "like");

        assertThat(reactionService.getPendingReactionDelta(ReactionEntity.POST, post.getId())).isNotZero();

        reactionService.syncToDatabase();

        assertThat(reactionService.getPendingReactionDelta(ReactionEntity.POST, post.getId())).isZero();
    }
}