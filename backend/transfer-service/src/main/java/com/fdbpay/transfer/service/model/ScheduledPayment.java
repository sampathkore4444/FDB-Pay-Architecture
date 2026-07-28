package com.fdbpay.transfer.service.model;

import com.fdbpay.transfer.service.model.enums.PaymentFrequency;
import com.fdbpay.transfer.service.model.enums.ScheduledPaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scheduled_payments", indexes = {
        @Index(name = "idx_sp_user_id", columnList = "userId"),
        @Index(name = "idx_sp_status", columnList = "status"),
        @Index(name = "idx_sp_next_execution", columnList = "nextExecutionDate")
})
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String recipientIdentifier;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentFrequency frequency;

    private LocalDate nextExecutionDate;

    private LocalDate lastExecutionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduledPaymentStatus status = ScheduledPaymentStatus.ACTIVE;

    private String description;

    @Builder.Default
    @Column(nullable = false)
    private int totalExecutions = 12;

    @Builder.Default
    @Column(nullable = false)
    private int completedExecutions = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum TransactionType {
        P2P,
        BILL_PAY
    }
}
