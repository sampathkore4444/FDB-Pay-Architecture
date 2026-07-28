package com.fdbpay.promotions.service.repository;

import com.fdbpay.promotions.service.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {

    List<PromotionUsage> findByUserIdAndPromotionId(UUID userId, UUID promotionId);

    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.userId = :userId AND pu.promotionId = :promotionId")
    long countByUserIdAndPromotionId(@Param("userId") UUID userId, @Param("promotionId") UUID promotionId);
}
