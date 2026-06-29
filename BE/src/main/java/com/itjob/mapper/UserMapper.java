package com.itjob.mapper;

import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RoleMapper.class, SkillMapper.class})
public interface UserMapper {
    @Mapping(target = "id", expression = "java(user.getId().toString())")
    UserResponse toUserResponse(User user);
}
