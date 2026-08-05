package com.fdbpay.transfer.service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkVoidRequest {

    @NotNull(message = "Transaction ids are required")
    @NotEmpty(message = "At least one transaction id is required")
    private List<UUID> transactionIds;
}
