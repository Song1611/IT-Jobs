package com.itjob.mapper;

import com.itjob.dto.response.RoleResponse;
import com.itjob.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {
    RoleResponse toRoleResponse(Role role);
}
