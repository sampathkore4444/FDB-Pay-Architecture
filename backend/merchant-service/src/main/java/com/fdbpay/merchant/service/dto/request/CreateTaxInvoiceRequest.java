package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaxInvoiceRequest {

    private String customerName;

    private String customerPhone;

    @NotNull(message = "Subtotal is required")
    @Positive(message = "Subtotal must be positive")
    private Long subtotal;

    @PositiveOrZero(message = "Tax must be non-negative")
    private Long tax;

    @PositiveOrZero(message = "Withholding tax must be non-negative")
    private Long withholdingTax;

    private LocalDate issueDate;
}
