package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String channel;

    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private String triggerEvent;

    private Boolean enabled;
}
