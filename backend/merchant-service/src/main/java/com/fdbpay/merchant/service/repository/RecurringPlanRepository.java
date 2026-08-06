package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.RecurringPlan;
import com.fdbpay.merchant.service.model.enums.RecurringPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RecurringPlanRepository extends JpaRepository<RecurringPlan, UUID> {
    List<RecurringPlan> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<RecurringPlan> findByStatusAndNextRunAtLessThanEqual(RecurringPlanStatus status, OffsetDateTime when);
}
