package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.WebhookDeliveryStatus;
import com.fdbpay.merchant.service.model.enums.WebhookEvent;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryResponse {

    private UUID id;
    private UUID subscriptionId;
    private WebhookEvent event;
    private String url;
    private String payload;
    private WebhookDeliveryStatus status;
    private Integer attempts;
    private Integer statusCode;
    private String error;
    private OffsetDateTime createdAt;
    private OffsetDateTime deliveredAt;
}
