package com.fdbpay.transfer.service.model;

import com.fdbpay.transfer.service.model.enums.TransactionStatus;
import com.fdbpay.transfer.service.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_tx_sender", columnList = "senderWalletId"),
        @Index(name = "idx_tx_receiver", columnList = "receiverWalletId"),
        @Index(name = "idx_tx_status", columnList = "status"),
        @Index(name = "idx_tx_created_at", columnList = "createdAt")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false)
    private UUID senderWalletId;

    @Column(nullable = false)
    private UUID receiverWalletId;

    @Column(nullable = false)
    private Long amount;

    @Builder.Default
    @Column(nullable = false)
    private Long fee = 0L;

    @Column(nullable = false, length = 3)
    private String currency;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    private UUID parentTransactionId;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime completedAt;

    private String failureReason;
}
