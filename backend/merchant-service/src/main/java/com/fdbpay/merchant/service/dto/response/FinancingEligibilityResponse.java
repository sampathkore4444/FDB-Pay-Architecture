package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancingEligibilityResponse {

    private boolean eligible;
    private Long monthlyRevenue;
    private Long threeMonthVolume;
    private Long avgDailySales;
    private Long estimatedLimit;
    private Integer maxTermMonths;
    private String message;
}
