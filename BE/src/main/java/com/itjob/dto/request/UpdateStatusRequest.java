package com.itjob.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateStatusRequest {
    
    @NotBlank(message = "Status is required")
    String status;
    
    String notes;
    
    String reason;
}
