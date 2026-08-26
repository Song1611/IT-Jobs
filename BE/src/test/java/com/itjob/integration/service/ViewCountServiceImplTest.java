package com.itjob.integration.service;

import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.ViewEntity;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.PostRepository;
import com.itjob.service.impl.ViewCountServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("IT - ViewCountService")
class ViewCountServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ViewCountServiceImpl viewCountService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("incrementView + syncToDatabase -> persists the pending view count")
    void incrementAndSyncPersistsViewCount() {
        // Arrange
        Company company = savedCompany("View Co");

        // Act
        viewCountService.incrementView(ViewEntity.COMPANY, company.getId());
        viewCountService.syncToDatabase();

        // Assert
        Company refreshed = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(refreshed.getViewCount()).isEqualTo(1L);
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.COMPANY, company.getId())).isZero();
    }

    @Test
    @DisplayName("incrementView -> counts each distinct viewer, not duplicate views from one viewer")
    void incrementViewCountsDistinctViewers() {
        // Arrange
        Company company = savedCompany("Viewers Co");
        UUID companyId = company.getId();

        // Act
        viewCountService.incrementView(ViewEntity.COMPANY, companyId, "viewer-1");
        viewCountService.incrementView(ViewEntity.COMPANY, companyId, "viewer-2");
        viewCountService.incrementView(ViewEntity.COMPANY, companyId, "viewer-3");
        viewCountService.incrementView(ViewEntity.COMPANY, companyId, "viewer-1");
        viewCountService.syncToDatabase();

        // Assert
        assertThat(companyRepository.findById(companyId).orElseThrow().getViewCount()).isEqualTo(3L);
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.COMPANY, companyId)).isZero();
    }

    @Test
    @DisplayName("incrementView without viewerId -> debounces nothing, every call counts")
    void incrementViewWithoutViewerIdCountsEveryCall() {
        // Arrange
        Company company = savedCompany("NoViewer Co");
        UUID companyId = company.getId();

        // Act
        viewCountService.incrementView(ViewEntity.COMPANY, companyId);
        viewCountService.incrementView(ViewEntity.COMPANY, companyId);
        viewCountService.syncToDatabase();

        // Assert
        assertThat(companyRepository.findById(companyId).orElseThrow().getViewCount()).isEqualTo(2L);
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.COMPANY, companyId)).isZero();
    }

    @Test
    @DisplayName("incrementView on JOB + syncToDatabase -> persists the view to the job")
    void incrementJobViewAndSyncPersists() {
        // Arrange
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Viewed Job");

        // Act
        viewCountService.incrementView(ViewEntity.JOB, jobId);
        viewCountService.syncToDatabase();

        // Assert
        Job job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getViewCount()).isEqualTo(1);
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.JOB, jobId)).isZero();
    }

    @Test
    @DisplayName("incrementView on POST + syncToDatabase -> persists the view")
    void incrementPostViewAndSyncPersists() {
        // Arrange
        User author = createVerifiedUser("author-" + UUID.randomUUID() + "@example.com");
        Post post = postRepository.save(Post.builder().author(author).content("Test post").build());
        UUID postId = post.getId();

        // Act
        viewCountService.incrementView(ViewEntity.POST, postId);
        viewCountService.syncToDatabase();

        // Assert
        Post refreshed = postRepository.findById(postId).orElseThrow();
        assertThat(refreshed.getViewCount()).isEqualTo(1);
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.POST, postId)).isZero();
    }

    @Test
    @DisplayName("syncToDatabase -> removes stale view key when the entity no longer exists")
    void syncRemovesStaleKeyForMissingEntity() {
        // Arrange
        UUID missingId = UUID.randomUUID();

        // Act
        viewCountService.incrementView(ViewEntity.JOB, missingId);
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.JOB, missingId)).isEqualTo(1L);
        viewCountService.syncToDatabase();

        // Assert
        assertThat(viewCountService.getPendingViewDelta(ViewEntity.JOB, missingId)).isZero();
    }

    private Company savedCompany(String name) {
        return companyRepository.save(Company.builder()
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue())
                .build());
    }
}
