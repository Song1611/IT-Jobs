package com.itjob.mapper;

import com.itjob.dto.response.PermissionResponse;
import com.itjob.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toPermissionResponse(Permission permission);
}
