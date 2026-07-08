package com.itjob.mapper;

import com.itjob.dto.response.PermissionResponse;
import com.itjob.entity.Permission;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class)
public interface PermissionMapper {
    
    PermissionResponse toPermissionResponse(Permission permission);
}
