package com.fdbpay.remittance.service.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemittanceRateQuoteResponse {

    private String corridor;
    private Long amount;
    private BigDecimal exchangeRate;
    private Long fee;
    private Long amountMmk;
}
