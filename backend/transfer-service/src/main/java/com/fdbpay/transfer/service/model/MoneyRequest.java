package com.fdbpay.transfer.service.model;

import com.fdbpay.transfer.service.model.enums.MoneyRequestStatus;
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
@Table(name = "money_requests", indexes = {
        @Index(name = "idx_mr_requester", columnList = "requesterUserId"),
        @Index(name = "idx_mr_target_phone", columnList = "targetPhone"),
        @Index(name = "idx_mr_status", columnList = "status")
})
public class MoneyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID requesterUserId;

    @Column(nullable = false)
    private String targetPhone;

    @Column(nullable = false)
    private Long amount;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MoneyRequestStatus status = MoneyRequestStatus.PENDING;

    private UUID paymentId;

    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
