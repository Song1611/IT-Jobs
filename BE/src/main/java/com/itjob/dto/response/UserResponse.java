package com.itjob.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itjob.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    String id;
    String fullName;
    String email;
    String phone;
    Gender gender;
    LocalDate dateOfBirth;
    String avatar;
    String coverImage;
    String cvUrl;
    String address;
    Set<RoleResponse> roles;
    Set<SkillResponse> skills;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}

