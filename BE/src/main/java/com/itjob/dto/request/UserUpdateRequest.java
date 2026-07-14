package com.itjob.dto.request;

import com.itjob.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    String fullName;
    
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email;
    
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password;
    
    @Size(max = 15, message = "Phone must not exceed 15 characters")
    String phone;
    
    Gender gender;
    
    LocalDate dateOfBirth;
    
    String avatar;
    
    String coverImage;
    
    String cvUrl;
    
    String address;
    
    Set<UUID> skillIds;
    
    // Only ADMIN can update roles
    Set<String> roles;
}

