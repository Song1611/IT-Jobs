package com.itjob.integration.service;

import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.PostRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.PostCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - PostCacheService")
class PostCacheServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private PostCacheService postCacheService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("getCachedPostDetail -> returns the cached post")
    void getCachedPostDetailReturnsPost() {
        User author = userRepository.save(User.builder()
                .fullName("Author").email("author-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("x")).enabled(true).build());
        Post post = postRepository.save(Post.builder().author(author).content("Hello").build());

        var response = postCacheService.getCachedPostDetail(post.getId());

        assertThat(response.getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("getCachedPostDetail -> throws POST_NOT_FOUND for a missing post")
    void getCachedPostDetailNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> postCacheService.getCachedPostDetail(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("evictPost and evictAll -> do not throw")
    void evictDoesNotThrow() {
        User author = userRepository.save(User.builder()
                .fullName("Author").email("author-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("x")).enabled(true).build());
        Post post = postRepository.save(Post.builder().author(author).content("Hello").build());

        assertThatCode(() -> postCacheService.evictPost(post.getId())).doesNotThrowAnyException();
        assertThatCode(() -> postCacheService.evictAll()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getCachedPostList -> returns cached posts")
    @Transactional
    void getCachedPostListReturnsPosts() {
        User author = userRepository.save(User.builder()
                .fullName("Author").email("author-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("x")).enabled(true).build());
        postRepository.save(Post.builder().author(author).content("Listed post").build());

        var page = postCacheService.getCachedPostList(org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("getCachedPostList -> returns empty items for a page with no content")
    @Transactional
    void getCachedPostListEmptyPageReturnsEmpty() {
        var page = postCacheService.getCachedPostList(PageRequest.of(999, 10));

        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("getCachedPostsByUser -> returns the user's cached posts")
    @Transactional
    void getCachedPostsByUserReturnsPosts() {
        User author = userRepository.save(User.builder()
                .fullName("Author").email("author-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("x")).enabled(true).build());
        postRepository.save(Post.builder().author(author).content("User post").build());

        var page = postCacheService.getCachedPostsByUser(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("getCachedPostsByCompany -> returns the company's cached posts")
    @Transactional
    void getCachedPostsByCompanyReturnsPosts() {
        User author = userRepository.save(User.builder()
                .fullName("Author").email("author-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("x")).enabled(true).build());
        com.itjob.entity.Company company = companyRepository.save(com.itjob.entity.Company.builder()
                .name("Co").slug("co-" + UUID.randomUUID()).status(com.itjob.enums.CompanyStatus.ACTIVE.getValue()).build());
        postRepository.save(Post.builder().author(author).company(company).content("Company post").build());

        var page = postCacheService.getCachedPostsByCompany(company.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }
}