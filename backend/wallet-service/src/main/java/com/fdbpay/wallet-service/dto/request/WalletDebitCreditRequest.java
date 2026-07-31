package com.fdbpay.wallet.service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletDebitCreditRequest {

    @NotNull(message = "walletId is required")
    private UUID walletId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Long amount;

    private String description;

    @NotNull(message = "txnId is required")
    private UUID txnId;
}
