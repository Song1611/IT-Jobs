package com.itjob.integration.service;

import com.itjob.config.AbstractIntegrationTest;
import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.request.JobRequest;
import com.itjob.dto.request.RegisterRequest;
import com.itjob.dto.request.VerifyEmailRequest;
import com.itjob.entity.User;
import com.itjob.enums.JobStatus;
import com.itjob.repository.UserRepository;
import com.itjob.service.AuthenticationService;
import com.itjob.service.CompanyService;
import com.itjob.service.EmailService;
import com.itjob.service.JobService;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Shared base for @SpringBootTest service integration tests: provides a real
 * register + OTP-verify flow (EmailService mocked) so tests get a fully verified
 * user persisted in PostgreSQL, plus helpers to create an active company + open job.
 */
public abstract class AbstractServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected AuthenticationService authenticationService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected CompanyService companyService;

    @Autowired
    protected JobService jobService;

    @MockitoBean
    protected EmailService emailService;

    protected User createVerifiedUser(String email) {
        RegisterRequest register = new RegisterRequest();
        register.setEmail(email);
        register.setPassword("password123");
        register.setFullName("Candidate");
        authenticationService.register(register);

        VerifyEmailRequest verify = new VerifyEmailRequest();
        verify.setEmail(email);
        verify.setOtp(capturedOtp(email));
        authenticationService.verifyEmail(verify);

        return userRepository.findByEmail(email).orElseThrow();
    }

    /**
     * Creates an already-enabled user directly in PostgreSQL (bypasses the OTP flow).
     * Use {@link #createVerifiedUser(String)} when the test must reflect the real
     * register -> verify business flow.
     */
    protected User createEnabledUser(String email) {
        return userRepository.save(User.builder()
                .fullName("Candidate")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .build());
    }

    /**
     * Creates an already-enabled admin user directly in PostgreSQL.
     */
    protected User createAdmin() {
        return userRepository.save(User.builder()
                .fullName("Admin")
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("admin123"))
                .enabled(true)
                .build());
    }

    /**
     * Creates an employer whose company has been approved by a separate admin,
     * mirroring the real business flow (EMPLOYER creates, ADMIN approves).
     */
    protected User employerWithActiveCompany() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        User admin = createAdmin();

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var company = companyService.createCompany(companyRequest("Active Co"), employer.getId());

        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        companyService.approveCompany(company.getId(), admin.getId());
        return employer;
    }

    protected UUID activeCompanyId(User employer) {
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        return companyService.getMyCompany(employer.getId()).getId();
    }

    protected UUID createOpenJob(User employer, String title) {
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        UUID companyId = companyService.getMyCompany(employer.getId()).getId();
        return jobService.createJob(companyId, jobRequest(title), employer.getId()).getId();
    }

    protected CompanyRequest companyRequest(String name) {
        CompanyRequest request = new CompanyRequest();
        request.setName(name);
        return request;
    }

    protected JobRequest jobRequest(String title) {
        JobRequest request = new JobRequest();
        request.setTitle(title);
        request.setDescription("Test description");
        request.setWorkLocation("Ho Chi Minh");
        request.setQuantity(1);
        request.setStatus(JobStatus.OPEN.getValue());
        return request;
    }

    protected String capturedOtp(String email) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerifyEmail(eq(email), captor.capture());
        return captor.getValue();
    }
}
