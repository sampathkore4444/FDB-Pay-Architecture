package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
