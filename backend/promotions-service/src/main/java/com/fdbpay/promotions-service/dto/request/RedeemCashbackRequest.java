package com.fdbpay.promotions.service.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedeemCashbackRequest {

    @Positive(message = "Amount must be positive")
    private Long amount;
}
