package com.fdbpay.kyc.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminKycRequestResponse {

    private String id;
    private UUID userId;
    private String userName;
    private String userPhone;
    private String documentType;
    private String status;
    private OffsetDateTime submittedAt;
    private String documentUrl;
}
