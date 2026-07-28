package com.fdbpay.corporate.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkDisbursementResponse {

    private UUID id;
    private String status;
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private OffsetDateTime createdAt;
}
