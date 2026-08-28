package com.itjob.integration.service;

import com.itjob.dto.request.BlogRequest;
import com.itjob.dto.response.BlogResponse;
import com.itjob.entity.Blog;
import com.itjob.entity.BlogCategory;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.BlogCategoryRepository;
import com.itjob.repository.BlogRepository;
import com.itjob.service.BlogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - BlogService")
class BlogServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogCategoryRepository categoryRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Test
    @DisplayName("createBlog -> creates a blog for the user")
    void createBlogCreates() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        BlogCategory category = newCategory();

        BlogResponse response = blogService.createBlog(user.getId(), blogRequest(category.getId()));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo("My Post");
        assertThat(response.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("createBlog -> throws BLOG_CATEGORY_NOT_FOUND for a missing category")
    void createBlogCategoryNotFoundThrows() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");

        UUID randomCategoryId = UUID.randomUUID();
        UUID userId = user.getId();
        BlogRequest request = blogRequest(randomCategoryId);
        assertThatThrownBy(() -> blogService.createBlog(userId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("getBlogById -> returns the blog")
    void getBlogByIdReturnsBlog() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        BlogCategory category = newCategory();
        BlogResponse created = blogService.createBlog(user.getId(), blogRequest(category.getId()));

        BlogResponse response = blogService.getBlogById(created.getId());

        assertThat(response.getTitle()).isEqualTo("My Post");
        assertThat(response.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getBlogById -> throws BLOG_NOT_FOUND for a missing blog")
    void getBlogByIdNotFoundThrows() {
        assertThatThrownBy(() -> blogService.getBlogById(UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteBlog -> soft-deletes the blog")
    void deleteBlogSoftDeletes() {
        User user = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        BlogCategory category = newCategory();
        BlogResponse created = blogService.createBlog(user.getId(), blogRequest(category.getId()));

        blogService.deleteBlog(created.getId(), user.getId());

        Blog persisted = blogRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getIsDeleted()).isTrue();
        assertThat(persisted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateBlog -> throws FORBIDDEN when the user is not the owner")
    void updateBlogByOtherUserThrows() {
        User owner = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(owner.getId(), owner.getEmail(), "USER");
        BlogCategory category = newCategory();
        BlogResponse created = blogService.createBlog(owner.getId(), blogRequest(category.getId()));

        User other = createVerifiedUser("other-" + UUID.randomUUID() + "@example.com");
        authenticateAs(other.getId(), other.getEmail(), "USER");

        UUID blogId = created.getId();
        UUID otherId = other.getId();
        BlogRequest request = blogRequest(category.getId());
        assertThatThrownBy(() -> blogService.updateBlog(blogId, otherId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private BlogCategory newCategory() {
        return categoryRepository.save(BlogCategory.builder().name("Tech-" + UUID.randomUUID()).build());
    }

    private BlogRequest blogRequest(UUID categoryId) {
        BlogRequest request = new BlogRequest();
        request.setCategoryId(categoryId);
        request.setTitle("My Post");
        request.setContent("Full content");
        return request;
    }
}