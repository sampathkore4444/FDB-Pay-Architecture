package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.PaymentLink;
import com.fdbpay.merchant.service.model.enums.PaymentLinkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, UUID> {

    Optional<PaymentLink> findByToken(String token);

    Page<PaymentLink> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<PaymentLink> findByMerchantIdAndStatus(UUID merchantId, PaymentLinkStatus status);

    boolean existsByToken(String token);

    @Modifying
    @Query("update PaymentLink p set p.status = :status, p.paidAt = :paidAt where p.id = :id")
    int markPaid(@Param("id") UUID id, @Param("status") PaymentLinkStatus status, @Param("paidAt") java.time.OffsetDateTime paidAt);

    @Query("select p from PaymentLink p where p.status = 'ACTIVE' and p.autoFollowUp = true and p.customerPhone is not null and (p.nextReminderAt is null or p.nextReminderAt <= :now)")
    List<PaymentLink> findDueForFollowUp(@Param("now") java.time.OffsetDateTime now);
}
