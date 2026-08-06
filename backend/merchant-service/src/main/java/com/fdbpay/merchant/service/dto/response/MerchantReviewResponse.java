package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ReviewStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantReviewResponse {

    private UUID id;
    private UUID merchantId;
    private String customerName;
    private String customerPhone;
    private Integer rating;
    private String comment;
    private ReviewStatus status;
    private String adminReply;
    private OffsetDateTime createdAt;
}
