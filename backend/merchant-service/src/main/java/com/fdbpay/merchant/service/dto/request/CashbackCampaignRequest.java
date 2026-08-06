package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    private String name;

    @NotNull(message = "Percent is required")
    @Positive(message = "Percent must be positive")
    private Integer percent;

    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private Long budget;

    private OffsetDateTime startsAt;

    private OffsetDateTime endsAt;

    @PositiveOrZero
    private Long spent;
}
