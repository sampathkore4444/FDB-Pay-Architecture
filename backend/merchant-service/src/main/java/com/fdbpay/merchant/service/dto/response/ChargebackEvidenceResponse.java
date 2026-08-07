package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargebackEvidenceResponse {

    private UUID id;
    private UUID chargebackId;
    private String type;
    private String reference;
    private String content;
    private OffsetDateTime createdAt;
}
