package com.fdbpay.transfer.service.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRow {

    private LocalDate date;
    private Long grossSales;
    private int saleCount;
    private Long refundAmount;
    private int refundCount;
    private Long fees;
    private Long netSales;
    private String settlementRef;
    private OffsetDateTime settledAt;
    private String status;
}
