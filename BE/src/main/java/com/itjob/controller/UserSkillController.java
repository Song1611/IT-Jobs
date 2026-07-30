package com.itjob.controller;

import com.itjob.dto.request.AddUserSkillRequest;
import com.itjob.dto.request.UpdateUserSkillRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.SkillResponse;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.SkillRepository;
import com.itjob.repository.UserRepository;
import com.itjob.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserSkillController {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    @GetMapping("/{userId}/skills")
    public ApiResponse<List<SkillResponse>> getUserSkills(@PathVariable UUID userId) {
        log.debug("Getting skills for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<SkillResponse> skills = toSkillResponses(user.getSkills());
        return ApiResponse.<List<SkillResponse>>builder()
                .message("User skills retrieved successfully")
                .result(skills)
                .build();
    }

    @GetMapping("/me/skills")
    public ApiResponse<List<SkillResponse>> getMySkills() {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.debug("Getting skills for current user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<SkillResponse> skills = toSkillResponses(user.getSkills());
        return ApiResponse.<List<SkillResponse>>builder()
                .message("Your skills retrieved successfully")
                .result(skills)
                .build();
    }

    @PostMapping("/me/skills")
    public ApiResponse<List<SkillResponse>> addSkill(
            @Valid @RequestBody AddUserSkillRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.debug("Adding skill {} for user {}", request.getSkillId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));

        user.getSkills().add(skill);
        userRepository.save(user);

        List<SkillResponse> skills = toSkillResponses(user.getSkills());
        return ApiResponse.<List<SkillResponse>>builder()
                .message("Skill added successfully")
                .result(skills)
                .build();
    }

    @PatchMapping("/me/skills/{skillId}")
    public ApiResponse<List<SkillResponse>> updateSkill(
            @PathVariable UUID skillId,
            @RequestBody UpdateUserSkillRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.debug("Updating skill {} for user {}: level={}, years={}",
                skillId, userId, request.getLevel(), request.getYearsOfExperience());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean hasSkill = user.getSkills().stream().anyMatch(s -> s.getId().equals(skillId));
        if (!hasSkill) {
            throw new AppException(ErrorCode.SKILL_NOT_FOUND);
        }

        List<SkillResponse> skills = toSkillResponses(user.getSkills());
        return ApiResponse.<List<SkillResponse>>builder()
                .message("Skill updated successfully")
                .result(skills)
                .build();
    }

    @DeleteMapping("/me/skills/{skillId}")
    public ApiResponse<List<SkillResponse>> removeSkill(@PathVariable UUID skillId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.debug("Removing skill {} from user {}", skillId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));

        user.getSkills().remove(skill);
        userRepository.save(user);

        List<SkillResponse> skills = toSkillResponses(user.getSkills());
        return ApiResponse.<List<SkillResponse>>builder()
                .message("Skill removed successfully")
                .result(skills)
                .build();
    }

    private List<SkillResponse> toSkillResponses(java.util.Set<Skill> skills) {
        if (skills == null || skills.isEmpty()) return List.of();
        return skills.stream()
                .map(s -> SkillResponse.builder()
                        .id(s.getId().toString())
                        .name(s.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
