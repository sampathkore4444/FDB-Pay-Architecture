package com.fdbpay.settlement.service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementSummaryResponse {

    private Long totalSettled;
    private Long totalFees;
    private int merchantCount;
    private UUID batchId;
}
