package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralProgramRequest {

    @NotBlank(message = "Code is required")
    private String code;

    private Long referralBonus;

    private Long referredBonus;
}
