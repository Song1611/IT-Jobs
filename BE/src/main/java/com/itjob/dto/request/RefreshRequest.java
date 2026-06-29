package com.itjob.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshRequest {
    @NotBlank(message = "REFRESH_TOKEN_REQUIRED")
    private String refreshToken;
}
