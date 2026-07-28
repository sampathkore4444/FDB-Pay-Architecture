package com.fdbpay.promotions.service.model;

import com.fdbpay.promotions.service.model.enums.CashbackTxnType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cashback_transactions")
public class CashbackTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID cashbackWalletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashbackTxnType type;

    @Column(nullable = false)
    private Long amount;

    private UUID promotionId;

    private UUID transactionId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
