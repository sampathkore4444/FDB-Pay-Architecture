package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDetailResponse {

    private String phone;
    private String name;
    private Long totalSpent;
    private int orderCount;
    private int refundCount;
    private Long refundedAmount;
    private long avgOrderValue;
    private int lastOrderDaysAgo;
    private boolean churnRisk;
    private Map<String, Long> byMonth;
}
