package com.fdbpay.auth.dto.response;

import com.fdbpay.auth.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserProfileResponse user;

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(UserProfileResponse.from(user))
                .build();
    }
}
