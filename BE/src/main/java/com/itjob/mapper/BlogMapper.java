package com.itjob.mapper;

import com.itjob.dto.request.BlogRequest;
import com.itjob.dto.response.BlogBriefResponse;
import com.itjob.dto.response.BlogResponse;
import com.itjob.entity.Blog;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class, uses = {UserMapper.class, BlogCategoryMapper.class})
public interface BlogMapper {
    
    @Mapping(target = "author", source = "user")
    BlogResponse toBlogResponse(Blog blog);
    
    @Mapping(target = "author", source = "user")
    BlogBriefResponse toBlogBriefResponse(Blog blog);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Blog toBlog(BlogRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateBlog(@MappingTarget Blog blog, BlogRequest request);
}
