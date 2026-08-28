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
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> blogService.getBlogById(randomId))
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

    @Test
    @DisplayName("getAllCategories -> returns all categories")
    void getAllCategoriesReturnsCategories() {
        BlogCategory category = newCategory();

        var result = blogService.getAllCategories();

        assertThat(result).extracting("id").contains(category.getId());
    }

    @Test
    @DisplayName("getRecentBlogs -> returns recent blogs")
    void getRecentBlogsReturnsBlogs() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();
        blogService.createBlog(author.getId(), blogRequest(category.getId()));

        var result = blogService.getRecentBlogs(7);

        assertThat(result).extracting("title").contains("My Post");
    }

    @Test
    @DisplayName("getRecentBlogs -> throws INVALID_LIMIT for a non-positive limit")
    void getRecentBlogsInvalidLimitThrows() {
        assertThatThrownBy(() -> blogService.getRecentBlogs(0))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LIMIT);
    }

    @Test
    @DisplayName("getRecentBlogs -> throws LIMIT_EXCEEDED above the max limit")
    void getRecentBlogsLimitExceededThrows() {
        assertThatThrownBy(() -> blogService.getRecentBlogs(101))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("getAllBlogs -> returns blogs excluding deleted ones")
    void getAllBlogsReturnsBlogs() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();
        BlogResponse created = blogService.createBlog(author.getId(), blogRequest(category.getId()));
        blogService.deleteBlog(created.getId(), author.getId());

        blogService.createBlog(author.getId(), blogRequest(category.getId()));

        var page = blogService.getAllBlogs(null, org.springframework.data.domain.PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("getBlogsByCategory -> returns blogs of a category")
    void getBlogsByCategoryReturnsBlogs() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();
        blogService.createBlog(author.getId(), blogRequest(category.getId()));

        var page = blogService.getBlogsByCategory(category.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getBlogsByCategory -> throws BLOG_CATEGORY_NOT_FOUND for a missing category")
    void getBlogsByCategoryNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> blogService.getBlogsByCategory(randomId, org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("getMyBlogs -> returns the user's blogs")
    void getMyBlogsReturnsBlogs() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();
        blogService.createBlog(author.getId(), blogRequest(category.getId()));

        var page = blogService.getMyBlogs(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateBlog -> updates title, category and regenerates the slug")
    void updateBlogUpdatesTitleAndCategory() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory oldCategory = newCategory();
        BlogCategory newCategory = newCategory();
        BlogResponse created = blogService.createBlog(author.getId(), blogRequest(oldCategory.getId()));

        BlogRequest request = blogRequest(newCategory.getId());
        request.setTitle("Updated Title");
        BlogResponse updated = blogService.updateBlog(created.getId(), author.getId(), request);

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getCategory().getName()).isEqualTo(newCategory.getName());
        assertThat(updated.getSlug()).isNotEqualTo(created.getSlug());
    }

    @Test
    @DisplayName("updateBlog -> throws BLOG_NOT_FOUND for a missing blog")
    void updateBlogNotFoundThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();

        UUID randomId = UUID.randomUUID();
        UUID authorId = author.getId();
        BlogRequest request = blogRequest(category.getId());
        assertThatThrownBy(() -> blogService.updateBlog(randomId, authorId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_FOUND);
    }

    @Test
    @DisplayName("updateBlog -> throws BLOG_CATEGORY_NOT_FOUND when changing to a missing category")
    void updateBlogCategoryNotFoundThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();
        BlogResponse created = blogService.createBlog(author.getId(), blogRequest(category.getId()));

        UUID blogId = created.getId();
        UUID authorId = author.getId();
        BlogRequest request = blogRequest(UUID.randomUUID());
        assertThatThrownBy(() -> blogService.updateBlog(blogId, authorId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("createBlog -> appends a suffix when the slug collides")
    void createBlogSlugCollisionAppendsSuffix() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        authenticateAs(author.getId(), author.getEmail(), "USER");
        BlogCategory category = newCategory();
        BlogRequest request = blogRequest(category.getId());
        request.setTitle("Same Title");
        BlogResponse first = blogService.createBlog(author.getId(), request);
        BlogResponse second = blogService.createBlog(author.getId(), request);

        assertThat(second.getSlug()).isNotEqualTo(first.getSlug());
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