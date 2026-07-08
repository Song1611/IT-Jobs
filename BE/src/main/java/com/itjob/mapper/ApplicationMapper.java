package com.itjob.mapper;

import com.itjob.dto.request.ApplicationRequest;
import com.itjob.dto.response.ApplicationResponse;
import com.itjob.entity.Application;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class, uses = {JobMapper.class, UserMapper.class})
public interface ApplicationMapper {
    
    @Mapping(target = "job", source = "job")
    @Mapping(target = "candidate", source = "user")
    ApplicationResponse toApplicationResponse(Application application);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "interviewAt", ignore = true)
    @Mapping(target = "respondedAt", ignore = true)
    @Mapping(target = "hrNotes", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "viewedByEmployer", ignore = true)
    @Mapping(target = "viewedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Application toApplication(ApplicationRequest request);
}
