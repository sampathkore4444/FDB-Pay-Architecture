package com.fdbpay.schemas.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserProfileResponse user;
}
