package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.SettlementType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantRegisterRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    private String businessType;

    private String businessLicense;

    private String taxId;

    private String settlementAccount;

    private SettlementType settlementType;

    private String feeSchedule;

    private String category;

    private String address;
}
