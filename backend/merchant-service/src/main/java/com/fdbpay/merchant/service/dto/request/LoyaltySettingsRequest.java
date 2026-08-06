package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltySettingsRequest {

    @NotNull(message = "Points per MMK is required")
    @PositiveOrZero
    private Integer pointsPerMmk;

    @NotNull(message = "Reward threshold is required")
    @Positive
    private Integer rewardThresholdPoints;

    @NotNull(message = "Reward value is required")
    @Positive
    private Long rewardValue;

    private Boolean enabled;
}
