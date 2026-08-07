package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.RefundStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {

    private UUID id;
    private UUID orderId;
    private UUID transactionId;
    private String customerPhone;
    private Long amount;
    private String reason;
    private RefundStatus status;
    private OffsetDateTime createdAt;
}
