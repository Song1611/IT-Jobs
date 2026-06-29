package com.itjob.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationRequest {
    @NotBlank(message = "USERNAME_REQUIRED")
    private String username;

    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;
}
