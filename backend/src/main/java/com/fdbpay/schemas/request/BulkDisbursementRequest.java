package com.fdbpay.schemas.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BulkDisbursementRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "File reference is required")
    private String fileRef;

    private String description;
}
