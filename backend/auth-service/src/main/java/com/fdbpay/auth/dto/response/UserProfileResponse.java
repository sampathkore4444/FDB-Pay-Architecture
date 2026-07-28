package com.fdbpay.auth.dto.response;

import com.fdbpay.auth.model.User;
import com.fdbpay.auth.model.enums.KycTier;
import com.fdbpay.auth.model.enums.UserRole;
import com.fdbpay.auth.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID id;
    private String phone;
    private String name;
    private String email;
    private UserStatus status;
    private KycTier kycTier;
    private UserRole role;
    private String referralCode;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus())
                .kycTier(user.getKycTier())
                .role(user.getRole())
                .referralCode(user.getReferralCode())
                .build();
    }
}
