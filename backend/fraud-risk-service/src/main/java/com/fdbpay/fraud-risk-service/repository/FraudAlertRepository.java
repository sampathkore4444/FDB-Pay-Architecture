package com.fdbpay.fraud.risk.service.repository;

import com.fdbpay.fraud.risk.service.model.FraudAlert;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    Page<FraudAlert> findByStatus(AlertStatus status, Pageable pageable);

    List<FraudAlert> findByTransactionId(UUID transactionId);
}
