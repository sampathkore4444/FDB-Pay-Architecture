package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.StaffAccount;
import com.fdbpay.merchant.service.model.enums.StaffAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAccountRepository extends JpaRepository<StaffAccount, UUID> {

    List<StaffAccount> findByMerchantId(UUID merchantId);

    Optional<StaffAccount> findByMerchantIdAndUserId(UUID merchantId, UUID userId);

    List<StaffAccount> findByMerchantIdAndStatus(UUID merchantId, StaffAccountStatus status);
}
