package com.itjob.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyResponse {
    UUID id;
    String name;
    String slug;
    String email;
    String phone;
    String website;
    String companySize;
    String industry;
    String nationality;
    Integer foundedYear;
    String workModes;
    String employmentTypes;
    String avatar;
    String coverImage;
    String description;
    String benefits;
    String address;
    String status;
    LocalDateTime verifiedAt;
    Integer viewCount;
    Integer followerCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // Additional fields
    Integer jobCount;
    Boolean isFollowed;
}
