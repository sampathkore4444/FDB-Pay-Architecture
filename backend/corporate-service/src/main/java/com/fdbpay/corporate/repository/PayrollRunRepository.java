package com.fdbpay.corporate.repository;

import com.fdbpay.corporate.model.PayrollRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Page<PayrollRun> findByCorporateUserIdOrderByCreatedAtDesc(UUID corporateUserId, Pageable pageable);
}
