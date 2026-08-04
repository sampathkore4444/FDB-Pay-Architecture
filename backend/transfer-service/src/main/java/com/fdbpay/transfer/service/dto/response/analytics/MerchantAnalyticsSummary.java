package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantAnalyticsSummary {

    private Long totalSales;
    private int saleCount;
    private Long avgTransactionValue;
    private int refundCount;
    private Long refundAmount;
    private Long netSales;
    private List<PaymentMethodBreakdown> paymentMethods;
    private List<DailyPoint> dailySeries;
    private List<TopCustomer> topCustomers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodBreakdown {
        private String method;
        private int count;
        private Long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPoint {
        private String date;
        private int count;
        private Long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCustomer {
        private String counterpartyWalletId;
        private int count;
        private Long amount;
    }
}
