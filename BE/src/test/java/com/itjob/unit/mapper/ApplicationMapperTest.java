package com.itjob.unit.mapper;

import com.itjob.dto.response.ApplicationResponse;
import com.itjob.entity.Application;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.mapper.ApplicationMapper;
import com.itjob.mapper.JobMapper;
import com.itjob.mapper.UserMapper;
import com.itjob.mapper.CompanyMapper;
import com.itjob.mapper.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - ApplicationMapper")
class ApplicationMapperTest {

    private final ApplicationMapper mapper = Mappers.getMapper(ApplicationMapper.class);

    @BeforeEach
    void wireUsedMappers() {
        JobMapper jobMapper = Mappers.getMapper(JobMapper.class);
        ReflectionTestUtils.setField(jobMapper, "companyMapper", Mappers.getMapper(CompanyMapper.class));
        ReflectionTestUtils.setField(jobMapper, "skillMapper", Mappers.getMapper(SkillMapper.class));
        ReflectionTestUtils.setField(mapper, "jobMapper", jobMapper);
        ReflectionTestUtils.setField(mapper, "userMapper", Mappers.getMapper(UserMapper.class));
    }

    @Test
    @DisplayName("toApplicationResponse -> maps job and candidate from the user")
    void toApplicationResponseMapsJobAndCandidate() {
        Job job = Job.builder().id(UUID.randomUUID()).title("Job A").build();
        User candidate = User.builder().id(UUID.randomUUID()).email("candidate@example.com").build();
        Application application = Application.builder().job(job).user(candidate).build();

        ApplicationResponse response = mapper.toApplicationResponse(application);

        assertThat(response.getJob().getId()).isEqualTo(job.getId());
        assertThat(response.getCandidate().getEmail()).isEqualTo("candidate@example.com");
    }
}