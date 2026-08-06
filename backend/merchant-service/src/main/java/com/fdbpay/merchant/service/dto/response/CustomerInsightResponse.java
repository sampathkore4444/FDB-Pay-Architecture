package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInsightResponse {

    private String walletId;
    private Long totalSpend;
    private Integer transactionCount;
    private OffsetDateTime lastPurchaseAt;
    private String tier;
    private Long loyaltyPoints;
}
