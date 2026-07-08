package com.itjob.mapper;

import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.UserBriefResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {
    
    UserResponse toUserResponse(User user);
    
    UserBriefResponse toUserBriefResponse(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // Email should not be updated
    @Mapping(target = "password", ignore = true) // Password handled separately
    @Mapping(target = "roles", ignore = true) // Roles handled separately
    @Mapping(target = "skills", ignore = true) // Skills handled separately
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
