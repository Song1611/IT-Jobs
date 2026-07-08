package com.itjob.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationRequest {
    
    @NotNull(message = "Job ID is required")
    UUID jobId;
    
    String cvUrl;
    
    String coverLetter;
}
