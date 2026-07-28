package com.fdbpay.wallet.service.dto.response;

import com.fdbpay.wallet.service.model.enums.KycTier;
import com.fdbpay.wallet.service.model.enums.WalletStatus;
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
    private WalletStatus status;
    private Long balanceTotal;
    private Long balanceAvailable;
    private Long balanceHeld;
    private Long balanceFrozen;
    private Long dailyLimit;
    private Long monthlyLimit;
    private KycTier kycTier;
    private OffsetDateTime createdAt;
}
