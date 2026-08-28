package com.itjob.integration.service;

import com.itjob.dto.response.PostResponse;
import com.itjob.entity.Company;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.PostRepository;
import com.itjob.service.PostService;
import com.itjob.service.storage.CloudinaryService;
import com.itjob.service.storage.CloudinaryUploadResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("IT - PostService")
class PostServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Test
    @DisplayName("create -> creates a post without a company")
    void createPostWithoutCompany() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        PostResponse response = postService.create(author.getId(), "Hello world", null, null, null);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getContent()).isEqualTo("Hello world");
        assertThat(response.getUser().getEmail()).isEqualTo(author.getEmail());
    }

    @Test
    @DisplayName("create -> throws POST_CONTENT_REQUIRED for blank content")
    void createPostBlankContentThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID authorId = author.getId();
        assertThatThrownBy(() -> postService.create(authorId, "   ", null, null, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_CONTENT_REQUIRED);
    }

    @Test
    @DisplayName("create -> throws COMPANY_NOT_FOUND for a missing company")
    void createPostCompanyNotFoundThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID authorId = author.getId();
        UUID randomCompanyId = UUID.randomUUID();
        assertThatThrownBy(() -> postService.create(authorId, "content", randomCompanyId, null, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("create -> allows the company creator to post")
    void createPostByCompanyCreator() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Company company = companyRepository.save(Company.builder()
                .name("My Co").slug("my-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue()).createdBy(author).build());

        PostResponse response = postService.create(author.getId(), "Company post", company.getId(), null, null);

        assertThat(response.getCompany().getId()).isEqualTo(company.getId());
    }

    @Test
    @DisplayName("create -> throws FORBIDDEN when the user is neither creator nor member")
    void createPostForbiddenWhenNotMember() {
        User owner = createVerifiedUser("owner-" + UUID.randomUUID() + "@example.com");
        Company company = companyRepository.save(Company.builder()
                .name("Their Co").slug("their-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue()).createdBy(owner).build());
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID companyId = company.getId();
        UUID authorId = author.getId();
        assertThatThrownBy(() -> postService.create(authorId, "content", companyId, null, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("update -> updates the post content")
    void updatePostUpdatesContent() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        PostResponse created = postService.create(author.getId(), "Original", null, null, null);

        PostResponse updated = postService.update(created.getId(), author.getId(), "Updated");

        assertThat(updated.getContent()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("update -> throws POST_NOT_FOUND for a missing post")
    void updatePostNotFoundThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID randomId = UUID.randomUUID();
        UUID authorId = author.getId();
        assertThatThrownBy(() -> postService.update(randomId, authorId, "content"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("update -> throws FORBIDDEN for a non-owner")
    void updatePostForbiddenForOtherUser() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        PostResponse created = postService.create(author.getId(), "Original", null, null, null);
        User other = createVerifiedUser("other-" + UUID.randomUUID() + "@example.com");

        UUID postId = created.getId();
        UUID otherId = other.getId();
        assertThatThrownBy(() -> postService.update(postId, otherId, "content"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("delete -> deletes the post")
    void deletePostDeletes() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        PostResponse created = postService.create(author.getId(), "To delete", null, null, null);

        postService.delete(created.getId(), author.getId());

        assertThat(postRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("getById -> returns the post with interaction")
    void getPostByIdReturnsPost() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        PostResponse created = postService.create(author.getId(), "Fetch me", null, null, null);

        PostResponse response = postService.getById(created.getId());

        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getContent()).isEqualTo("Fetch me");
        assertThat(response.getInteraction()).isNotNull();
        assertThat(response.getInteraction().getTotalLikes()).isZero();
    }

    @Test
    @DisplayName("getAll -> returns posts")
    void getAllPostsReturnsPosts() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        postService.create(author.getId(), "Post A", null, null, null);

        var page = postService.getAll(org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("getByUser -> returns the user's posts")
    void getByUserReturnsPosts() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        postService.create(author.getId(), "My Post", null, null, null);

        var page = postService.getByUser(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("getByCompany -> returns the company's posts")
    void getByCompanyReturnsPosts() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Company company = companyRepository.save(Company.builder()
                .name("My Co").slug("my-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue()).createdBy(author).build());
        postService.create(author.getId(), "Company Post", company.getId(), null, null);

        var page = postService.getByCompany(company.getId(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("getById -> throws POST_NOT_FOUND for a missing post")
    void getPostByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> postService.getById(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("create -> throws POST_CONTENT_REQUIRED for null content")
    void createPostNullContentThrows() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");

        UUID authorId = author.getId();
        assertThatThrownBy(() -> postService.create(authorId, null, null, null, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.POST_CONTENT_REQUIRED);
    }

    @Test
    @DisplayName("create -> throws FORBIDDEN when the company has no creator and user is not a member")
    void createPostCompanyNoCreatorForbidden() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Company company = companyRepository.save(Company.builder()
                .name("No Creator Co").slug("nc-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue()).build());

        UUID companyId = company.getId();
        UUID authorId = author.getId();
        assertThatThrownBy(() -> postService.create(authorId, "content", companyId, null, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("create -> uploads an image attachment via Cloudinary")
    void createPostWithImageUploadsAttachment() {
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        when(cloudinaryService.upload(any(), anyString()))
                .thenReturn(new CloudinaryUploadResult("https://cdn/img.png", "img-1", "image"));
        MockMultipartFile image = new MockMultipartFile("file", "img.png", "image/png", new byte[]{1});

        PostResponse response = postService.create(author.getId(), "With image", null, List.of(image), null);

        assertThat(response.getAttachments()).hasSize(1);
        assertThat(response.getAttachments().get(0).getFileUrl()).isEqualTo("https://cdn/img.png");
    }
}