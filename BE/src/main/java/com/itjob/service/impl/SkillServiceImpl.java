package com.itjob.service.impl;

import com.itjob.redis.CacheName;
import com.itjob.dto.response.SkillResponse;
import com.itjob.entity.Skill;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.SkillMapper;
import com.itjob.repository.SkillRepository;
import com.itjob.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Override
    @Cacheable(value = CacheName.SKILL_LIST, key = "'list'")
    public List<SkillResponse> getAllSkills() {
        log.info("Cache MISS - Fetching all skills from database");
        return skillRepository.findAll().stream()
                .map(skillMapper::toSkillResponse)
                .toList();
    }

    @Override
    @Cacheable(value = CacheName.SKILL_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    public SkillResponse getSkillById(UUID id) {
        log.info("Cache MISS - Fetching skill {} from database", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));
        return skillMapper.toSkillResponse(skill);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheName.SKILL_LIST, allEntries = true)
    public SkillResponse createSkill(String name) {
        log.debug("Creating skill: {}", name);

        if (skillRepository.existsByName(name)) {
            throw new AppException(ErrorCode.SKILL_ALREADY_EXISTS);
        }

        Skill skill = Skill.builder()
                .name(name)
                .build();
        skill = skillRepository.save(skill);

        log.debug("Skill created: {}", name);
        return skillMapper.toSkillResponse(skill);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheName.SKILL_LIST, allEntries = true),
        @CacheEvict(value = CacheName.SKILL_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    })
    public SkillResponse updateSkill(UUID id, String name) {
        log.debug("Updating skill {}: {}", id, name);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));

        if (!skill.getName().equals(name) && skillRepository.existsByName(name)) {
            throw new AppException(ErrorCode.SKILL_ALREADY_EXISTS);
        }

        skill.setName(name);
        skill = skillRepository.save(skill);

        log.debug("Skill updated: {}", name);
        return skillMapper.toSkillResponse(skill);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheName.SKILL_LIST, allEntries = true),
        @CacheEvict(value = CacheName.SKILL_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    })
    public void deleteSkill(UUID id) {
        log.debug("Deleting skill: {}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));

        skillRepository.delete(skill);

        log.debug("Skill deleted: {}", id);
    }
}
