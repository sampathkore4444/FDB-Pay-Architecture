package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {

    private UUID id;
    private String name;
    private String channel;
    private String subject;
    private String body;
    private String triggerEvent;
    private boolean enabled;
    private OffsetDateTime createdAt;
}
