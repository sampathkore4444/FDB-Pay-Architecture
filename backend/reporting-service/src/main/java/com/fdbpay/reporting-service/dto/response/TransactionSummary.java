package com.fdbpay.reporting.service.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummary {

    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalCount;
    private BigDecimal totalAmount;
    private Map<String, Long> byType;
}
