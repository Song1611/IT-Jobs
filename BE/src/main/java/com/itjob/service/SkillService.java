package com.itjob.service;

import com.itjob.dto.response.SkillResponse;

import java.util.List;
import java.util.UUID;

public interface SkillService {

    List<SkillResponse> getAllSkills();

    SkillResponse getSkillById(UUID id);

    SkillResponse createSkill(String name);

    SkillResponse updateSkill(UUID id, String name);

    void deleteSkill(UUID id);
}
