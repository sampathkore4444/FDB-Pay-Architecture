package com.fdbpay.reporting.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetrics {

    private Long totalTransactions;
    private Long activeUsers;
    private Long totalVolume;
    private Double successRate;
}
