package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.WebhookEvent;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscriptionResponse {

    private UUID id;
    private WebhookEvent event;
    private String url;
    private boolean enabled;
    private OffsetDateTime createdAt;
}
