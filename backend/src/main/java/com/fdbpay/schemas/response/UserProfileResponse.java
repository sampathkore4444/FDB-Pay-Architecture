package com.fdbpay.schemas.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID id;
    private String phone;
    private String name;
    private String email;
    private String status;
    private String kycTier;
    private String role;
    private String referralCode;
}
