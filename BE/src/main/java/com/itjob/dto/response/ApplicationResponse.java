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
public class ApplicationResponse {
    UUID id;
    String cvUrl;
    String coverLetter;
    String status;
    LocalDateTime appliedAt;
    LocalDateTime reviewedAt;
    LocalDateTime interviewAt;
    LocalDateTime respondedAt;
    String hrNotes;
    String rejectionReason;
    Integer rating;
    Boolean viewedByEmployer;
    LocalDateTime viewedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // Job info
    JobBriefResponse job;
    
    // Candidate info (for HR view)
    UserBriefResponse candidate;
}
