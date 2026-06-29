package com.itjob.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private String refreshToken;
    private String accessToken;
    private boolean authenticated;
}
