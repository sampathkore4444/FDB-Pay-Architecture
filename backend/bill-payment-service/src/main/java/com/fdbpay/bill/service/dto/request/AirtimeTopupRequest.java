package com.fdbpay.bill.service.dto.request;

import com.fdbpay.bill.service.model.enums.AirtimeProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirtimeTopupRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @NotNull(message = "Provider is required")
    private AirtimeProvider provider;
}
