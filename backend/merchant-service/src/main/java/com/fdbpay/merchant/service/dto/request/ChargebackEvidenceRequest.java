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
public class ChargebackEvidenceRequest {

    @NotNull(message = "Chargeback id is required")
    private UUID chargebackId;

    @NotBlank(message = "Type is required")
    private String type;

    private String reference;

    private String content;
}
