package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeBreakdown {
    private Long transactionFees;
    private Long cardFees;
    private Long refundFees;
    private Long serviceFees;
}
