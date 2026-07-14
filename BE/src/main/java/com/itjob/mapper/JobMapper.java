package com.itjob.mapper;

import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.JobBriefResponse;
import com.itjob.dto.response.JobResponse;
import com.itjob.entity.Job;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class, uses = {CompanyMapper.class, SkillMapper.class})
public interface JobMapper {
    
    /**
     * Map Job entity to JobResponse with Company and Skills
     */
    @Mapping(target = "company", source = "company")
    @Mapping(target = "skills", source = "skills")
    JobResponse toJobResponse(Job job);
    
    /**
     * Map Job entity to JobResponse with Company only (no Skills)
     * Used for listings where skills are not needed
     */
    @Mapping(target = "company", source = "company")
    @Mapping(target = "skills", ignore = true)
    JobResponse toJobResponseWithCompanyOnly(Job job);
    
    @Mapping(target = "company", source = "company")
    JobBriefResponse toJobBriefResponse(Job job);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "applicationCount", ignore = true)
    Job toJob(JobRequest request);
    
    @InheritConfiguration(name = "toJob")
    void updateJob(@MappingTarget Job job, JobRequest request);
}
