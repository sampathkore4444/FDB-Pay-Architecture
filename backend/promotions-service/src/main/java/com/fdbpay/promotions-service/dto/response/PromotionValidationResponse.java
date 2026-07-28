package com.fdbpay.promotions.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionValidationResponse {

    private boolean valid;
    private Long discount;
    private String message;
}
