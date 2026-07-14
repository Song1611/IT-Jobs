package com.itjob.mapper;

import com.itjob.dto.response.BlogCategoryResponse;
import com.itjob.entity.BlogCategory;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface BlogCategoryMapper {
    
    BlogCategoryResponse toBlogCategoryResponse(BlogCategory category);
}
