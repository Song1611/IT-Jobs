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
public class ReviewResponse {
    UUID id;
    UUID companyId;
    String companyName;
    String userName;
    Integer rating;
    Integer salaryRating;
    Integer cultureRating;
    Integer managementRating;
    Integer workLifeBalanceRating;
    String title;
    String pros;
    String cons;
    String advice;
    String comment;
    Boolean isVerifiedEmployee;
    String workPosition;
    String workDuration;
    String status;
    Boolean isAnonymous;
    Integer helpfulCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
