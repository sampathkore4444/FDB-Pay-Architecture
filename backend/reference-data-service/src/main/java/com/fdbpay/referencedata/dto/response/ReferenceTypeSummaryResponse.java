package com.fdbpay.referencedata.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceTypeSummaryResponse {

    private UUID id;
    private String code;
    private String description;
    private Boolean active;
    private Long valueCount;
    private OffsetDateTime updatedAt;
}
