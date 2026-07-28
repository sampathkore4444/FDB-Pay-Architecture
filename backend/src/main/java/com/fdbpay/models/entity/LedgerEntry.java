package com.fdbpay.models.entity;

import com.fdbpay.models.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private UUID txnId;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;
}
