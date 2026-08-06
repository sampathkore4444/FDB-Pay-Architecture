package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantReviewRequest {

    private String customerName;

    private String customerPhone;

    @NotNull(message = "Rating is required")
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}
