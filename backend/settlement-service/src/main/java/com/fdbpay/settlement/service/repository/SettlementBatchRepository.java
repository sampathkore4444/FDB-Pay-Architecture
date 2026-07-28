package com.fdbpay.settlement.service.repository;

import com.fdbpay.settlement.service.model.SettlementBatch;
import com.fdbpay.settlement.service.model.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {

    Optional<SettlementBatch> findByStatus(BatchStatus status);

    Optional<SettlementBatch> findByBatchDate(LocalDate batchDate);
}
