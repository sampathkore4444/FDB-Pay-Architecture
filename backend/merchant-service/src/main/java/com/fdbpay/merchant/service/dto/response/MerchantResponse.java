package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import com.fdbpay.merchant.service.model.enums.SettlementType;
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
    private String businessLicense;
    private String taxId;
    private String settlementAccount;
    private SettlementType settlementType;
    private String feeSchedule;
    private MerchantStatus status;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private String qrStaticUrl;
    private Integer rollingReservePercent;
    private Integer rollingReservePeriodDays;
    private Long rollingReserveBalance;
    private String terminalFields;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
