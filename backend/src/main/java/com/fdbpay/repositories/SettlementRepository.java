package com.fdbpay.repositories;

import com.fdbpay.models.entity.Settlement;
import com.fdbpay.models.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Page<Settlement> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<Settlement> findByStatus(SettlementStatus status);

    List<Settlement> findByMerchantIdAndStatus(UUID merchantId, SettlementStatus status);
}
