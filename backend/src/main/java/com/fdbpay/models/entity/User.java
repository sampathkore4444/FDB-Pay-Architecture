package com.fdbpay.models.entity;

import com.fdbpay.models.enums.KycTier;
import com.fdbpay.models.enums.UserRole;
import com.fdbpay.models.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(length = 100)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String nrcNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KycTier kycTier = KycTier.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.CONSUMER;

    @Column(length = 200)
    private String pinHash;

    @Builder.Default
    private Integer pinAttempts = 0;

    private OffsetDateTime pinLockedUntil;

    @Column(unique = true, length = 10)
    private String referralCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by")
    private User referredBy;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
