package com.itjob.mapper;

import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.response.CompanyBriefResponse;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.entity.Company;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class)
public interface CompanyMapper {
    
    CompanyResponse toCompanyResponse(Company company);
    
    CompanyBriefResponse toCompanyBriefResponse(Company company);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Company toCompany(CompanyRequest request);
    
    @InheritConfiguration(name = "toCompany")
    void updateCompany(@MappingTarget Company company, CompanyRequest request);
}
