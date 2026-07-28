package com.fdbpay.transfer.service.dto.response;

import com.fdbpay.transfer.service.model.ScheduledPayment;
import com.fdbpay.transfer.service.model.enums.PaymentFrequency;
import com.fdbpay.transfer.service.model.enums.ScheduledPaymentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledPaymentResponse {

    private UUID id;
    private UUID userId;
    private String recipientIdentifier;
    private Long amount;
    private ScheduledPayment.TransactionType type;
    private PaymentFrequency frequency;
    private LocalDate nextExecutionDate;
    private LocalDate lastExecutionDate;
    private ScheduledPaymentStatus status;
    private String description;
    private int totalExecutions;
    private int completedExecutions;
    private OffsetDateTime createdAt;
}
