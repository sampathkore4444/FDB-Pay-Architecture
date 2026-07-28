package com.fdbpay.schemas.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantResponse {

    private UUID id;
    private UUID userId;
    private String businessName;
    private String businessType;
    private String category;
    private String status;
    private String settlementType;
    private String feeSchedule;
    private String address;
    private String qrStaticUrl;
    private OffsetDateTime createdAt;
}
