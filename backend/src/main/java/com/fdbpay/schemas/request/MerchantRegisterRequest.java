package com.fdbpay.schemas.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantRegisterRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    private String businessType;

    private String businessLicense;

    private String taxId;

    private String settlementAccount;

    private String category;

    private String address;
}
