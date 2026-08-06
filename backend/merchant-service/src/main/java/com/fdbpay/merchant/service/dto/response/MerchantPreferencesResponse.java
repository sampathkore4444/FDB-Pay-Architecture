package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantPreferencesResponse {

    private String settlementPreferredTime;
    private Long alertLargeOrderThreshold;
    private Long alertDailySurgeThreshold;
    private String webhookUrl;
}
