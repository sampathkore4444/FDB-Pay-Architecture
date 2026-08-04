package com.fdbpay.referencedata.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceTypeResponse {

    private UUID id;
    private String code;
    private String description;
    private Boolean active;
    private List<ReferenceValueResponse> values;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
