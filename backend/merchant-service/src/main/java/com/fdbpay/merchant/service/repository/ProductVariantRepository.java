package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(UUID productId);

    void deleteByProductId(UUID productId);

    long countByProductId(UUID productId);
}
