package com.itjob.mapper;

import com.itjob.dto.response.SkillResponse;
import com.itjob.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkillMapper {
    @Mapping(target = "id", expression = "java(skill.getId().toString())")
    SkillResponse toSkillResponse(Skill skill);
}
