package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepeatCustomerResponse {

    private String customerPhone;
    private int orderCount;
    private Long totalSpent;
    private double repeatRate;
}
