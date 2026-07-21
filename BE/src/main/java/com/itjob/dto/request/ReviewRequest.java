package com.itjob.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewRequest {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    Integer rating;

    @Min(value = 1, message = "Salary rating must be at least 1")
    @Max(value = 5, message = "Salary rating must not exceed 5")
    Integer salaryRating;

    @Min(value = 1, message = "Culture rating must be at least 1")
    @Max(value = 5, message = "Culture rating must not exceed 5")
    Integer cultureRating;

    @Min(value = 1, message = "Management rating must be at least 1")
    @Max(value = 5, message = "Management rating must not exceed 5")
    Integer managementRating;

    @Min(value = 1, message = "Work-life balance rating must be at least 1")
    @Max(value = 5, message = "Work-life balance rating must not exceed 5")
    Integer workLifeBalanceRating;

    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title;

    String pros;

    String cons;

    String advice;

    String comment;

    Boolean isVerifiedEmployee;

    @Size(max = 100, message = "Work position must not exceed 100 characters")
    String workPosition;

    @Size(max = 50, message = "Work duration must not exceed 50 characters")
    String workDuration;

    Boolean isAnonymous;
}
