package com.fdbpay.agent.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSummaryResponse {

    private Long totalEarned;
    private Long totalPaid;
    private Long pendingBalance;
    private List<CommissionRecordResponse> withdrawalHistory;
}
