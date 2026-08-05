package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantStatement {

    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private Long totalVolume;
    private int transactionCount;
    private FeeBreakdown feeBreakdown;
    private Long totalFees;
    private Long netSales;
    private int refundCount;
    private Long refundAmount;
    private List<GrossByType> grossByType;
    private RollingReserveInfo rollingReserve;
}
