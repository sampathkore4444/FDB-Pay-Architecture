package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscriptionRequest {

    @NotNull(message = "Event is required")
    private String event;

    @NotBlank(message = "URL is required")
    private String url;

    private Integer maxRetries;

    private Integer backoffMinutes;
}
