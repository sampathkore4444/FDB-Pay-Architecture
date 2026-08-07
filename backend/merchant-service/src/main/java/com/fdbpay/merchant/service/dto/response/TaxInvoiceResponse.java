package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxInvoiceResponse {

    private UUID id;
    private String invoiceNo;
    private String customerName;
    private String customerPhone;
    private Long subtotal;
    private Long tax;
    private Long withholdingTax;
    private Long total;
    private LocalDate issueDate;
    private OffsetDateTime createdAt;
}
