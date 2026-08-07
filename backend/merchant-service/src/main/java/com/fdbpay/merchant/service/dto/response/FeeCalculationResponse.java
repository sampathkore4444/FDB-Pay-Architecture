package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCalculationResponse {

    private Long amount;
    private Long fee;
    private Long net;
    private String feeSchedule;
    private double feeRate;
}
