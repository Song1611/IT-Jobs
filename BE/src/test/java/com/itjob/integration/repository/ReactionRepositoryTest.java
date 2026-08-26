package com.itjob.integration.repository;

import com.itjob.config.AbstractPostgresIntegrationTest;
import com.itjob.entity.Post;
import com.itjob.entity.Reaction;
import com.itjob.entity.User;
import com.itjob.fixture.TestDataFactory;
import com.itjob.repository.PostRepository;
import com.itjob.repository.ReactionRepository;
import com.itjob.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IT - ReactionRepository")
class ReactionRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save -> assigns createdAt")
    void saveAssignsCreatedAt() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post = savedPost(user);

        Reaction saved = reactionRepository.save(reaction(post, user, "like"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByPostIdAndUserId -> returns reaction when exists")
    void findByPostIdAndUserIdReturnsReaction() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post = savedPost(user);
        reactionRepository.save(reaction(post, user, "like"));

        Optional<Reaction> result = reactionRepository.findByPostIdAndUserId(post.getId(), user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getReactionType()).isEqualTo("like");
    }

    @Test
    @DisplayName("findByPostIdAndUserId -> empty when reaction does not exist")
    void findByPostIdAndUserIdReturnsEmpty() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post = savedPost(user);

        Optional<Reaction> result = reactionRepository.findByPostIdAndUserId(post.getId(), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByPostIdAndUserId -> true when user reacted")
    void existsByPostIdAndUserIdTrueWhenReacted() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post = savedPost(user);
        reactionRepository.save(reaction(post, user, "love"));

        boolean exists = reactionRepository.existsByPostIdAndUserId(post.getId(), user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByPostIdAndUserId -> false when user did not react")
    void existsByPostIdAndUserIdFalseWhenNotReacted() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post = savedPost(user);

        boolean exists = reactionRepository.existsByPostIdAndUserId(post.getId(), user.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByUserIdAndPostIdIn -> returns matching reactions with correct post and type")
    void findByUserIdAndPostIdIn() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post1 = savedPost(user);
        Post post2 = savedPost(user);
        Post post3 = savedPost(user);
        reactionRepository.save(reaction(post1, user, "haha"));
        reactionRepository.save(reaction(post2, user, "wow"));

        List<Reaction> result = reactionRepository.findByUserIdAndPostIdIn(
                user.getId(), List.of(post1.getId(), post2.getId(), post3.getId()));

        assertThat(result)
                .extracting(
                        reaction -> reaction.getPost().getId(),
                        Reaction::getReactionType)
                .containsExactlyInAnyOrder(
                        tuple(post1.getId(), "haha"),
                        tuple(post2.getId(), "wow"));
    }

    @Test
    @DisplayName("countByPostId -> counts reactions of a post")
    void countByPostId() {
        User user = TestDataFactory.user(userRepository, "author@example.com");
        Post post = savedPost(user);
        reactionRepository.save(reaction(post, user, "like"));
        User other = TestDataFactory.user(userRepository, "other-" + UUID.randomUUID() + "@example.com");
        reactionRepository.save(reaction(post, other, "love"));

        long count = reactionRepository.countByPostId(post.getId());

        assertThat(count).isEqualTo(2);
    }

    private Post savedPost(User author) {
        return postRepository.save(Post.builder().author(author).content("Test post content").build());
    }

    private Reaction reaction(Post post, User user, String type) {
        return Reaction.builder().post(post).user(user).reactionType(type).build();
    }
}