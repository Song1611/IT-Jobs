package com.itjob.mapper;

import com.itjob.dto.response.RoleResponse;
import com.itjob.entity.Role;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class, uses = {PermissionMapper.class})
public interface RoleMapper {
    
    RoleResponse toRoleResponse(Role role);
}
