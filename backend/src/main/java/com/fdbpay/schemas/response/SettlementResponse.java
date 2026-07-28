package com.fdbpay.schemas.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResponse {

    private UUID id;
    private UUID merchantId;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private Long grossAmount;
    private Long fees;
    private Long netAmount;
    private String status;
    private OffsetDateTime settledAt;
    private String settlementRef;
    private OffsetDateTime createdAt;
}
