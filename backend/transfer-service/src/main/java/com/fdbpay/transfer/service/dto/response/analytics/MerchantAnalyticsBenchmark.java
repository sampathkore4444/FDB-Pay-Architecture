package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantAnalyticsBenchmark {

    private Long merchantTotalSales;
    private int merchantSaleCount;
    private Long merchantAvgTransactionValue;
    private Long platformTotalSales;
    private int platformTransactionCount;
    private Long platformAvgTransactionValue;
    private Double vsAveragePercent;
}
