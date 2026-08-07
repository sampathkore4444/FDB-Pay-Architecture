package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowForecastResponse {

    private List<MonthPoint> months;
    private Long projectedAnnual;
    private Long averageMonthly;
    private int growthRatePct;
    private Long seasonalAdjustment;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthPoint {
        private String month;
        private Long revenue;
        private Long projection;
    }
}
