package com.itjob.mapper;

import com.itjob.dto.response.SkillResponse;
import com.itjob.entity.Skill;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.*;

@Mapper(config = CentralMapperConfig.class)
public interface SkillMapper {
    
    SkillResponse toSkillResponse(Skill skill);
}
