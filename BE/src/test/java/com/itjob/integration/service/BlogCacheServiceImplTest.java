package com.itjob.integration.service;

import com.itjob.entity.Blog;
import com.itjob.entity.BlogCategory;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.BlogCategoryRepository;
import com.itjob.repository.BlogRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.BlogCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - BlogCacheService")
class BlogCacheServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private BlogCacheService blogCacheService;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private BlogCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("getCachedBlogById -> returns the cached blog")
    void getCachedBlogByIdReturnsBlog() {
        User author = userRepository.save(User.builder()
                .fullName("Author").email("author-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("x")).enabled(true).build());
        BlogCategory category = categoryRepository.save(BlogCategory.builder().name("Cat-" + UUID.randomUUID()).build());
        Blog blog = blogRepository.save(Blog.builder().user(author).category(category).title("Post").content("Body").build());

        var response = blogCacheService.getCachedBlogById(blog.getId());

        assertThat(response.getTitle()).isEqualTo("Post");
    }

    @Test
    @DisplayName("getCachedBlogById -> throws BLOG_NOT_FOUND for a missing blog")
    void getCachedBlogByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> blogCacheService.getCachedBlogById(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_FOUND);
    }
}