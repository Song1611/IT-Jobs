package com.itjob.integration.service;

import com.itjob.entity.Skill;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.SkillRepository;
import com.itjob.service.SkillService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - SkillService")
class SkillServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private SkillService skillService;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    @DisplayName("getAllSkills -> returns all saved skills")
    void getAllSkillsReturnsSaved() {
        String name = "Listed-" + UUID.randomUUID();
        var skill = skillService.createSkill(name);

        var result = skillService.getAllSkills();

        assertThat(result).extracting("id").contains(skill.getId());
    }

    @Test
    @DisplayName("getSkillById -> returns the skill")
    void getSkillByIdReturnsSkill() {
        Skill skill = skillRepository.save(Skill.builder().name("Spring-" + UUID.randomUUID()).build());

        var result = skillService.getSkillById(skill.getId());

        assertThat(result.getName()).isEqualTo(skill.getName());
    }

    @Test
    @DisplayName("getSkillById -> throws SKILL_NOT_FOUND for a missing skill")
    void getSkillByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> skillService.getSkillById(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKILL_NOT_FOUND);
    }

    @Test
    @DisplayName("createSkill -> creates a new skill")
    void createSkillCreates() {
        String name = "Kotlin-" + UUID.randomUUID();

        var result = skillService.createSkill(name);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("createSkill -> throws SKILL_ALREADY_EXISTS for a duplicate name")
    void createSkillDuplicateThrows() {
        String name = "Duplicate-" + UUID.randomUUID();
        skillService.createSkill(name);

        assertThatThrownBy(() -> skillService.createSkill(name))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKILL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("updateSkill -> updates the name")
    void updateSkillUpdatesName() {
        Skill skill = skillRepository.save(Skill.builder().name("Old-" + UUID.randomUUID()).build());
        String newName = "New-" + UUID.randomUUID();

        var result = skillService.updateSkill(skill.getId(), newName);

        assertThat(result.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("updateSkill -> throws SKILL_NOT_FOUND for a missing skill")
    void updateSkillNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> skillService.updateSkill(randomId, "X"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKILL_NOT_FOUND);
    }

    @Test
    @DisplayName("updateSkill -> throws SKILL_ALREADY_EXISTS when renaming to an existing skill")
    void updateSkillDuplicateThrows() {
        String existingName = "Existing-" + UUID.randomUUID();
        skillService.createSkill(existingName);
        Skill skill = skillRepository.save(Skill.builder().name("Other-" + UUID.randomUUID()).build());

        assertThatThrownBy(() -> skillService.updateSkill(skill.getId(), existingName))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKILL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("deleteSkill -> removes the skill")
    void deleteSkillDeletes() {
        Skill skill = skillRepository.save(Skill.builder().name("ToDelete-" + UUID.randomUUID()).build());

        skillService.deleteSkill(skill.getId());

        assertThat(skillRepository.findById(skill.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteSkill -> throws SKILL_NOT_FOUND for a missing skill")
    void deleteSkillNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> skillService.deleteSkill(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SKILL_NOT_FOUND);
    }
}