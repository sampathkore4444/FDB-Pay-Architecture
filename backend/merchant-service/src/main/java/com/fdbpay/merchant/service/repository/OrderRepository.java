package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<Order> findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(UUID merchantId, String customerPhone);

    List<Order> findByMerchantIdAndStatusOrderByCreatedAtDesc(UUID merchantId, OrderStatus status);

    @Query("select o from Order o where o.merchantId = :merchantId and o.createdAt >= :from order by o.createdAt desc")
    List<Order> findByMerchantIdAndCreatedAtAfter(@Param("merchantId") UUID merchantId, @Param("from") OffsetDateTime from);

    @Query("select o from Order o where o.status in :statuses and o.merchantId = :merchantId")
    List<Order> findByMerchantIdAndStatusIn(UUID merchantId, List<OrderStatus> statuses);
}
