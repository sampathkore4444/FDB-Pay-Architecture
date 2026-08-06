package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutAccountRequest {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    private String branch;

    private Boolean isDefault;
}
