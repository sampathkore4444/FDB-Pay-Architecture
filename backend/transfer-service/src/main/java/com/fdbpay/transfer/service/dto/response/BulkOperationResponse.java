package com.fdbpay.transfer.service.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResponse {

    private int successCount;
    private int failedCount;
    private List<BulkOperationResult> results;
}
