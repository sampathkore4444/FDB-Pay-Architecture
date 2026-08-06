package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackCampaignResponse {

    private UUID id;
    private UUID merchantId;
    private String name;
    private Integer percent;
    private Long budget;
    private Long spent;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
}
