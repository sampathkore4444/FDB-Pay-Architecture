package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.DigitalDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DigitalDeliveryRepository extends JpaRepository<DigitalDelivery, UUID> {

    List<DigitalDelivery> findByMerchantIdOrderByDeliveredAtDesc(UUID merchantId);

    List<DigitalDelivery> findByOrderId(UUID orderId);
}
