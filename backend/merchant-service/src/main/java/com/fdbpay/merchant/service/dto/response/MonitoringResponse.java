package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringResponse {

    private long recentTransactions;
    private long failedTransactions;
    private long pendingRefunds;
    private double anomalyScore;
    private java.util.List<String> alerts;
}
