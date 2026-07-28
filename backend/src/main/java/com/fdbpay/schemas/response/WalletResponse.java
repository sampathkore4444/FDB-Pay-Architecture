package com.fdbpay.schemas.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {

    private UUID id;
    private String currency;
    private String status;
    private Long balanceTotal;
    private Long balanceAvailable;
    private Long balanceHeld;
    private Long balanceFrozen;
    private Long dailyLimit;
    private Long monthlyLimit;
    private String kycTier;
    private OffsetDateTime createdAt;
}
