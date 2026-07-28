package com.fdbpay.promotions.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackWalletResponse {

    private UUID id;
    private UUID userId;
    private Long balance;
    private Long totalEarned;
    private Long totalRedeemed;
    private OffsetDateTime updatedAt;
}
