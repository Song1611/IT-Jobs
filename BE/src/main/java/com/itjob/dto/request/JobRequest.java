package com.itjob.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    String title;
    
    @NotBlank(message = "Description is required")
    String description;
    
    String type; // full-time, part-time, contract, internship
    
    String level; // intern, junior, middle, senior, lead
    
    String experience; // e.g. "1-2 years"
    
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity;
    
    BigDecimal salaryMin;
    
    BigDecimal salaryMax;
    
    String salaryCurrency;
    
    String salaryType; // monthly, yearly, hourly
    
    Boolean isNegotiable;
    
    @NotBlank(message = "Work location is required")
    String workLocation;
    
    String benefits;
    
    String requirements;
    
    @Future(message = "Deadline must be in the future")
    LocalDate deadline;
    
    String status; // open, closed, draft
    
    Set<UUID> skillIds;
}
