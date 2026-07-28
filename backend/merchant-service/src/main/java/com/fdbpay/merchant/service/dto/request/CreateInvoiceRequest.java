package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {

    private String customerPhone;

    private String customerName;

    private String items;

    @NotNull(message = "Subtotal is required")
    private Long subtotal;

    @NotNull(message = "Tax is required")
    private Long tax;

    @NotNull(message = "Total is required")
    private Long total;

    private LocalDate dueDate;
}
