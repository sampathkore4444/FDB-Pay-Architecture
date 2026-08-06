package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    private String name;

    @NotNull(message = "Campaign type is required")
    private String campaignType;

    @NotBlank(message = "Audience segment is required")
    private String audienceSegment;

    private UUID discountCodeId;

    private UUID cashbackId;
}
