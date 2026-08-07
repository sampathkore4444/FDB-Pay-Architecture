package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<NotificationTemplate> findByMerchantIdAndTriggerEventOrderByCreatedAtDesc(UUID merchantId, String triggerEvent);

    List<NotificationTemplate> findByMerchantIdAndEnabledTrue(UUID merchantId);
}
