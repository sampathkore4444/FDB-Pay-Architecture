package com.fdbpay.bill.service.dto.response;

import com.fdbpay.bill.service.model.BillPayment;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPaymentResponse {

    private UUID id;
    private UUID userId;
    private UUID billerId;
    private String accountNumber;
    private Long amount;
    private String transactionRef;
    private BillPayment.PaymentStatus status;
    private OffsetDateTime createdAt;
}
