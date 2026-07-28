package com.fdbpay.promotions.service.dto.response;

import com.fdbpay.promotions.service.model.enums.FundingType;
import com.fdbpay.promotions.service.model.enums.PromotionStatus;
import com.fdbpay.promotions.service.model.enums.PromotionType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {

    private UUID id;
    private String title;
    private String description;
    private PromotionType type;
    private FundingType fundingType;
    private UUID merchantId;
    private Long discountValue;
    private Long maxDiscount;
    private Long minTransactionAmount;
    private Integer maxUsageTotal;
    private Integer maxUsagePerUser;
    private Integer usageCount;
    private Integer remainingUses;
    private Boolean isActive;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private PromotionStatus status;
    private String promoCode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
