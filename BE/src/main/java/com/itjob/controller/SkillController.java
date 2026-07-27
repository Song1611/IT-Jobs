package com.itjob.controller;

import com.itjob.dto.request.NameRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.SkillResponse;
import com.itjob.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Slf4j
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ApiResponse<List<SkillResponse>> getAllSkills() {
        log.info("Get all skills request");
        return ApiResponse.<List<SkillResponse>>builder()
                .result(skillService.getAllSkills())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillResponse> getSkillById(@PathVariable UUID id) {
        log.info("Get skill by id: {}", id);
        return ApiResponse.<SkillResponse>builder()
                .result(skillService.getSkillById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkillResponse> createSkill(@Valid @RequestBody NameRequest request) {
        log.info("Create skill: {}", request.getName());
        return ApiResponse.<SkillResponse>builder()
                .result(skillService.createSkill(request.getName()))
                .message("Skill created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SkillResponse> updateSkill(@PathVariable UUID id,
                                                   @Valid @RequestBody NameRequest request) {
        log.info("Update skill {}: {}", id, request.getName());
        return ApiResponse.<SkillResponse>builder()
                .result(skillService.updateSkill(id, request.getName()))
                .message("Skill updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSkill(@PathVariable UUID id) {
        log.info("Delete skill: {}", id);
        skillService.deleteSkill(id);
        return ApiResponse.<Void>builder()
                .message("Skill deleted successfully")
                .build();
    }
}
