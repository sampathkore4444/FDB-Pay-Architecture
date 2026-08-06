package com.fdbpay.merchant.service.model;

import com.fdbpay.merchant.service.model.enums.ReviewStatus;
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
@Table(name = "merchant_reviews", indexes = {
        @Index(name = "idx_review_merchant", columnList = "merchantId")
})
public class MerchantReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(length = 150)
    private String customerName;

    @Column(length = 20)
    private String customerPhone;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(columnDefinition = "text")
    private String adminReply;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
