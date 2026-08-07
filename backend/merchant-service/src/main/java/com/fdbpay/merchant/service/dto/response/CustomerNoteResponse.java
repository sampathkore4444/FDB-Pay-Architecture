package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNoteResponse {

    private UUID id;
    private String customerPhone;
    private String note;
    private String createdBy;
    private OffsetDateTime createdAt;
}
