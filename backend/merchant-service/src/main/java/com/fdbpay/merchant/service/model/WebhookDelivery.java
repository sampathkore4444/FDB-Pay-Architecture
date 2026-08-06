package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.WebhookDeliveryStatus;
import com.fdbpay.merchant.service.model.enums.WebhookEvent;
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
@Table(name = "webhook_deliveries", indexes = {
        @Index(name = "idx_whdel_merchant", columnList = "merchantId")
})
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WebhookEvent event;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookDeliveryStatus status;

    @Column(nullable = false)
    private Integer attempts;

    private Integer statusCode;

    @Column(length = 500)
    private String error;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime deliveredAt;
}
