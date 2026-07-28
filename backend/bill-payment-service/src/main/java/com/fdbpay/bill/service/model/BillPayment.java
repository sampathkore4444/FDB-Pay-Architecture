package com.fdbpay.bill.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_payments", indexes = {
        @Index(name = "idx_bp_user_id", columnList = "userId"),
        @Index(name = "idx_bp_biller_id", columnList = "billerId"),
        @Index(name = "idx_bp_account_number", columnList = "accountNumber"),
        @Index(name = "idx_bp_status", columnList = "status")
})
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID billerId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Long amount;

    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
