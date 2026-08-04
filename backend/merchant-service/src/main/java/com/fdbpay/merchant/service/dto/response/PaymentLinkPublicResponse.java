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
public class PaymentLinkPublicResponse {

    private String token;
    private UUID merchantId;
    private String merchantName;
    private Long amount;
    private String description;
    private PaymentLinkStatus status;
    private OffsetDateTime createdAt;
}
