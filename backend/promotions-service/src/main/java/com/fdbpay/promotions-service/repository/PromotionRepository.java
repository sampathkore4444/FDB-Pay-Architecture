package com.fdbpay.promotions.service.repository;

import com.fdbpay.promotions.service.model.Promotion;
import com.fdbpay.promotions.service.model.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    List<Promotion> findByStatus(PromotionStatus status);

    List<Promotion> findByMerchantId(UUID merchantId);

    Optional<Promotion> findByPromoCode(String promoCode);

    List<Promotion> findByStatusAndStartDateBeforeAndEndDateAfter(
            PromotionStatus status, OffsetDateTime now1, OffsetDateTime now2);
}
