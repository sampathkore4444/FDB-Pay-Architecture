package com.fdbpay.merchant.service.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantPreferencesRequest {

    private String settlementPreferredTime;

    private Long alertLargeOrderThreshold;

    private Long alertDailySurgeThreshold;

    private String webhookUrl;
}
