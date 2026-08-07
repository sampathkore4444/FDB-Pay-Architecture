package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.DigitalDeliveryStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalDeliveryResponse {

    private UUID id;
    private UUID merchantId;
    private UUID orderId;
    private UUID productId;
    private String content;
    private String deliveredTo;
    private DigitalDeliveryStatus status;
    private OffsetDateTime deliveredAt;
}
