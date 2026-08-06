package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInsight {

    private String walletId;
    private Long amount;
    private Integer count;
    private OffsetDateTime lastPurchaseAt;
}
