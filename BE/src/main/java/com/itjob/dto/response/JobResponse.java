package com.itjob.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobResponse {
    UUID id;
    String title;
    String slug;
    String description;
    String type;
    String level;
    String experience;
    Integer quantity;
    BigDecimal salaryMin;
    BigDecimal salaryMax;
    String salaryCurrency;
    String salaryType;
    Boolean isNegotiable;
    String workLocation;
    String benefits;
    String requirements;
    Integer viewCount;
    Integer applicationCount;
    LocalDate deadline;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // Company info
    CompanyBriefResponse company;
    
    // Skills
    Set<SkillResponse> skills;
    
    // Additional fields for job detail
    Boolean isApplied;
    Boolean isSaved;
}
