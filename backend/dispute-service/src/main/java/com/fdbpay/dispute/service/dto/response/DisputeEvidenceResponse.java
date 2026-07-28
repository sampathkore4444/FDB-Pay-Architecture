package com.fdbpay.dispute.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEvidenceResponse {

    private UUID id;
    private String fileUrl;
    private String description;
    private UUID uploadedBy;
    private OffsetDateTime createdAt;
}
