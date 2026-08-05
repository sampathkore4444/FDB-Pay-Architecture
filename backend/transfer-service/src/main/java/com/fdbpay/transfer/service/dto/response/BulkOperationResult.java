package com.fdbpay.transfer.service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResult {

    private UUID transactionId;
    private boolean success;
    private String message;
}
