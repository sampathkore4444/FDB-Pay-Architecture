package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.PayoutStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutResponse {

    private UUID id;
    private UUID accountId;
    private String accountLabel;
    private Long amount;
    private PayoutStatus status;
    private String reference;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
}
