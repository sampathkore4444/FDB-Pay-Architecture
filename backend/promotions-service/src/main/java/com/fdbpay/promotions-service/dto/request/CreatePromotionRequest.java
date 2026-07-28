package com.fdbpay.promotions.service.dto.request;

import com.fdbpay.promotions.service.model.enums.FundingType;
import com.fdbpay.promotions.service.model.enums.PromotionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromotionRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Promotion type is required")
    private PromotionType type;

    @NotNull(message = "Funding type is required")
    private FundingType fundingType;

    private UUID merchantId;

    @Positive(message = "Discount value must be positive")
    private Long discountValue;

    private Long maxDiscount;

    private Long minTransactionAmount;

    private Integer maxUsageTotal;

    private Integer maxUsagePerUser;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private String promoCode;
}
