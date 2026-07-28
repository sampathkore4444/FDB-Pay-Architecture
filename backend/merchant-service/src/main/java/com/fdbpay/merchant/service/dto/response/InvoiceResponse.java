package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.InvoiceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private UUID id;
    private UUID merchantId;
    private String customerPhone;
    private String customerName;
    private String items;
    private Long subtotal;
    private Long tax;
    private Long total;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}
