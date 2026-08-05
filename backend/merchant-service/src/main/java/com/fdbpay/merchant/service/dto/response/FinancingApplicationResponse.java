package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.FinancingStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancingApplicationResponse {

    private UUID id;
    private UUID merchantId;
    private Long requestedAmount;
    private Integer termMonths;
    private String purpose;
    private Long monthlyRevenue;
    private Long estimatedLimit;
    private FinancingStatus status;
    private String adminNote;
    private OffsetDateTime createdAt;
    private OffsetDateTime reviewedAt;
}
