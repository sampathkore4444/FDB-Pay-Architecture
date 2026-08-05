package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ChargebackReason;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargebackNoteResponse {

    private UUID id;
    private String authorType;
    private String authorName;
    private String message;
    private OffsetDateTime createdAt;
}
