package com.fdbpay.auth.model;

import com.fdbpay.auth.model.enums.KycTier;
import com.fdbpay.auth.model.enums.UserRole;
import com.fdbpay.auth.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
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

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 254)
    private String email;

    @Column(name = "nrc_number", length = 30)
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

    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "pin_attempts", nullable = false)
    @Builder.Default
    private Integer pinAttempts = 0;

    @Column(name = "pin_locked_until")
    private LocalDateTime pinLockedUntil;

    @Column(name = "referral_code", unique = true, length = 10)
    private String referralCode;

    @Column(name = "referred_by", length = 10)
    private String referredBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
