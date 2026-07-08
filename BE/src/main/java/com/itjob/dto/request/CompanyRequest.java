package com.itjob.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyRequest {
    
    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    String name;
    
    @Email(message = "Invalid email format")
    String email;
    
    @Pattern(regexp = "^[0-9+\\-() ]{7,15}$", message = "Invalid phone number")
    String phone;
    
    String website;
    
    String companySize; // '1-50', '51-200', '201-500', '501-1000', '1000+'
    
    String industry;
    
    String nationality;
    
    Integer foundedYear;
    
    String workModes; // JSON: ["on-site", "remote", "hybrid"]
    
    String employmentTypes; // JSON: ["full-time", "part-time", "contract", "internship"]
    
    String avatar;
    
    String coverImage;
    
    String description;
    
    String benefits;
    
    String address;
}
