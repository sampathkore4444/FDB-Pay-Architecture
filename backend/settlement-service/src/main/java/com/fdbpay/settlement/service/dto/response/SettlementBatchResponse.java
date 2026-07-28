package com.fdbpay.settlement.service.dto.response;

import com.fdbpay.settlement.service.model.enums.BatchStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementBatchResponse {

    private UUID id;
    private LocalDate batchDate;
    private int totalMerchants;
    private Long totalGrossAmount;
    private Long totalFees;
    private Long totalNetAmount;
    private BatchStatus status;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
}
