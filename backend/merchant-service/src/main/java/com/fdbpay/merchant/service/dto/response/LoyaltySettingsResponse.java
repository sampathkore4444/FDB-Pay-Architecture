package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltySettingsResponse {

    private Boolean enabled;
    private Integer pointsPerMmk;
    private Integer rewardThresholdPoints;
    private Long rewardValue;
}
