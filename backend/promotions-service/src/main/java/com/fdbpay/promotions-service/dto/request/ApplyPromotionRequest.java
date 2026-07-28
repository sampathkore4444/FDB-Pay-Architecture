package com.fdbpay.promotions.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyPromotionRequest {

    @NotBlank(message = "Promo code is required")
    private String promoCode;

    @Positive(message = "Transaction amount must be positive")
    private Long transactionAmount;
}
