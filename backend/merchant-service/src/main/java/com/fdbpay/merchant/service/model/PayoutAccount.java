package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
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
@Table(name = "payout_accounts", indexes = {
        @Index(name = "idx_pa_merchant", columnList = "merchantId")
})
public class PayoutAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 150)
    private String accountName;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 100)
    private String branch;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
