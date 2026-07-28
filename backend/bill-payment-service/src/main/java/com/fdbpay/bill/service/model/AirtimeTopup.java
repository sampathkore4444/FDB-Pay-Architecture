package com.fdbpay.bill.service.model;

import com.fdbpay.bill.service.model.enums.AirtimeProvider;
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
@Table(name = "airtime_topups", indexes = {
        @Index(name = "idx_at_user_id", columnList = "userId"),
        @Index(name = "idx_at_provider", columnList = "provider"),
        @Index(name = "idx_at_status", columnList = "status"),
        @Index(name = "idx_at_created_at", columnList = "createdAt")
})
public class AirtimeTopup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AirtimeProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TopupStatus status = TopupStatus.PENDING;

    private String transactionRef;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum TopupStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
