package com.itjob.unit.mapper;

import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.JobResponse;
import com.itjob.dto.response.SkillResponse;
import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.Skill;
import com.itjob.mapper.CompanyMapper;
import com.itjob.mapper.JobMapper;
import com.itjob.mapper.SkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - JobMapper")
class JobMapperTest {

    private final JobMapper mapper = Mappers.getMapper(JobMapper.class);

    @BeforeEach
    void wireUsedMappers() {
        ReflectionTestUtils.setField(mapper, "companyMapper", Mappers.getMapper(CompanyMapper.class));
        ReflectionTestUtils.setField(mapper, "skillMapper", Mappers.getMapper(SkillMapper.class));
    }

    @Test
    @DisplayName("toJobResponse -> maps company and skills")
    void toJobResponseMapsCompanyAndSkills() {
        Company company = Company.builder().name("Tech Corp").build();
        Skill java = Skill.builder().name("Java").build();
        Job job = Job.builder().title("SE").company(company).skills(Set.of(java)).id(UUID.randomUUID()).build();

        JobResponse response = mapper.toJobResponse(job);

        assertThat(response.getCompany().getName()).isEqualTo("Tech Corp");
        assertThat(response.getSkills()).extracting(SkillResponse::getName).containsExactly("Java");
    }

    @Test
    @DisplayName("toJob -> maps request fields, ignores generated fields")
    void toJobMapsRequestFields() {
        JobRequest request = new JobRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("Build APIs");

        Job job = mapper.toJob(request);

        assertThat(job.getId()).isNull();
        assertThat(job.getCompany()).isNull();
        assertThat(job.getTitle()).isEqualTo("Backend Engineer");
        assertThat(job.getDescription()).isEqualTo("Build APIs");
        assertThat(job.getViewCount()).isNull();
        assertThat(job.getApplicationCount()).isNull();
    }

    @Test
    @DisplayName("updateJob -> updates mutable fields, preserves ignored ones")
    void updateJobUpdatesFields() {
        Job job = Job.builder().title("Old Title").description("Old Desc").build();
        JobRequest request = new JobRequest();
        request.setTitle("New Title");
        request.setDescription("New Desc");

        mapper.updateJob(job, request);

        assertThat(job.getTitle()).isEqualTo("New Title");
        assertThat(job.getDescription()).isEqualTo("New Desc");
    }
}