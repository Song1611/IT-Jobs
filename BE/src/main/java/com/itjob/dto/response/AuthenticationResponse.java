package com.itjob.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String refreshToken;
    private String accessToken;
    private boolean authenticated;
}
