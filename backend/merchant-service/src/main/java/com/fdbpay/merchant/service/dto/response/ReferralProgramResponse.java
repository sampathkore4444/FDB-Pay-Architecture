package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralProgramResponse {

    private UUID id;
    private UUID merchantId;
    private String code;
    private Long referralBonus;
    private Long referredBonus;
    private Integer uses;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
}
