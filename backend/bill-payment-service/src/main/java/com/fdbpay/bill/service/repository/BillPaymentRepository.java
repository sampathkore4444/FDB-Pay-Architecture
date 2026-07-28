package com.fdbpay.bill.service.repository;

import com.fdbpay.bill.service.model.BillPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {

    Page<BillPayment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<BillPayment> findByBillerIdOrderByCreatedAtDesc(UUID billerId, Pageable pageable);
}
