package com.fdbpay.promotions.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionUsageResponse {

    private UUID id;
    private UUID promotionId;
    private UUID userId;
    private UUID transactionId;
    private Long discountApplied;
    private Long cashbackAmount;
    private OffsetDateTime createdAt;
}
