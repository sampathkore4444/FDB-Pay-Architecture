package com.fdbpay.settlement.service.dto.response;

import com.fdbpay.settlement.service.model.enums.SettlementStatus;
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
    private UUID batchId;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private Long grossAmount;
    private Long fees;
    private Long netAmount;
    private SettlementStatus status;
    private OffsetDateTime settledAt;
    private String settlementRef;
    private int transactionCount;
    private OffsetDateTime createdAt;
}
