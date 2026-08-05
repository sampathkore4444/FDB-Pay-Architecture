package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.ChargebackStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondChargebackRequest {

    @NotNull(message = "Status is required")
    private ChargebackStatus status;

    private String note;
}
