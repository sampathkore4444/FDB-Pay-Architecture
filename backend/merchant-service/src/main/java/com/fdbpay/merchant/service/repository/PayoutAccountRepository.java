package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.PayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutAccountRepository extends JpaRepository<PayoutAccount, UUID> {
    List<PayoutAccount> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
