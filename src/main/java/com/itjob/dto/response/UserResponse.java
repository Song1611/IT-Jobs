package com.itjob.dto.response;


import com.itjob.entity.Gender;
import com.itjob.entity.Role;
import com.itjob.entity.Skill;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String fullName;
    String email;
    String password;
    String phone;
    Gender gender;
    LocalDate dateOfBirth;
    String avatar;
    String coverImage;
    String cvUrl;
    String address;
    Set<Role> roles;
    Set<Skill> skills;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}
