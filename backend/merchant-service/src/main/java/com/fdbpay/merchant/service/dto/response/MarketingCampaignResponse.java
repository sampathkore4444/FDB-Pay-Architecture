package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.model.enums.CampaignType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingCampaignResponse {

    private UUID id;
    private String name;
    private CampaignType campaignType;
    private String audienceSegment;
    private UUID discountCodeId;
    private UUID cashbackId;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
}
