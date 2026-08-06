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
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_apikey_merchant", columnList = "merchantId")
})
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String keyHash;

    @Column(nullable = false, length = 20)
    private String keyPreview;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String environment = "LIVE";

    @Builder.Default
    @Column(nullable = false)
    private Long usageCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    private OffsetDateTime lastUsedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
