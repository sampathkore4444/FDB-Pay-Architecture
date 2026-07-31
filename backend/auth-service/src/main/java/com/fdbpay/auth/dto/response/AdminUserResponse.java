package com.fdbpay.auth.dto.response;

import com.fdbpay.auth.model.User;
import com.fdbpay.auth.model.enums.KycTier;
import com.fdbpay.auth.model.enums.UserRole;
import com.fdbpay.auth.model.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private UUID id;
    private String phone;
    private String name;
    private String email;
    private UserStatus status;
    private KycTier kycTier;
    private UserRole role;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus())
                .kycTier(user.getKycTier())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
