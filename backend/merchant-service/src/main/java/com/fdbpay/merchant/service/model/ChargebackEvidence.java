package com.fdbpay.merchant.service.model;

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
@Table(name = "chargeback_evidence", indexes = {
        @Index(name = "idx_cbe_chargeback", columnList = "chargebackId")
})
public class ChargebackEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID chargebackId;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(length = 255)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
