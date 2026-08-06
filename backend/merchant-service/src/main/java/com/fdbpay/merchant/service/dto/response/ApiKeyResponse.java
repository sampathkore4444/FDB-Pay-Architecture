package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private UUID id;
    private String name;
    private String keyPreview;
    private ActiveStatus status;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime createdAt;
}
