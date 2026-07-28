package com.fdbpay.kyc.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycStatusResponse {

    private UUID userId;
    private String tier;
    private String status;
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;
}
