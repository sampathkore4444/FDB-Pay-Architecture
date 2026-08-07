package com.fdbpay.merchant.service.repository;

import com.fdbpay.merchant.service.model.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, UUID> {

    List<CustomerNote> findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(UUID merchantId, String customerPhone);
}
