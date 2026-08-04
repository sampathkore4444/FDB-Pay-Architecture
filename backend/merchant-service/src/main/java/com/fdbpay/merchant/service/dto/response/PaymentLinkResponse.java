package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.PaymentLinkStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkResponse {

    private UUID id;
    private UUID merchantId;
    private String token;
    private Long amount;
    private String description;
    private String customerPhone;
    private String customerName;
    private PaymentLinkStatus status;
    private boolean singleUse;
    private OffsetDateTime paidAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
