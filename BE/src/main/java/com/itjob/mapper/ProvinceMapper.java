package com.itjob.mapper;

import com.itjob.dto.response.ProvinceResponse;
import com.itjob.entity.Province;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface ProvinceMapper {

    ProvinceResponse toProvinceResponse(Province province);
}
