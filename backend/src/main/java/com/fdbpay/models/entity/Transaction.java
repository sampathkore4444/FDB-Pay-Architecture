package com.fdbpay.models.entity;

import com.fdbpay.models.enums.TransactionStatus;
import com.fdbpay.models.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet")
    private Wallet senderWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet")
    private Wallet receiverWallet;

    @Column(nullable = false)
    private Long amount;

    @Builder.Default
    private Long fee = 0L;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "MMK";

    private String description;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime completedAt;

    private String failureReason;
}
