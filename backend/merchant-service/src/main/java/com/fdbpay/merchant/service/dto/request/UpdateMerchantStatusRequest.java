package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMerchantStatusRequest {

    @NotNull(message = "Status is required")
    private MerchantStatus status;

    private String reason;
}
