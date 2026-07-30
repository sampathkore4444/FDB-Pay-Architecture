package com.fdbpay.settlement.service.repository;

import com.fdbpay.settlement.service.model.Settlement;
import com.fdbpay.settlement.service.model.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Page<Settlement> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    List<Settlement> findByStatus(SettlementStatus status);

    List<Settlement> findByBatchId(UUID batchId);

    @Query("SELECT s FROM Settlement s WHERE s.periodStart >= :start AND s.periodEnd <= :end ORDER BY s.createdAt DESC")
    List<Settlement> findByPeriodBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT s FROM Settlement s WHERE s.merchantId = :merchantId AND s.periodStart >= :start AND s.periodEnd <= :end")
    List<Settlement> findByMerchantIdAndPeriodBetween(@Param("merchantId") UUID merchantId,
                                                      @Param("start") OffsetDateTime start,
                                                      @Param("end") OffsetDateTime end);

    @Query("SELECT COUNT(DISTINCT s.merchantId) FROM Settlement s WHERE s.batchId = :batchId")
    int countDistinctMerchantsByBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.batchId = :batchId")
    Long sumNetAmountByBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT COALESCE(SUM(s.fees), 0) FROM Settlement s WHERE s.batchId = :batchId")
    Long sumFeesByBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.batchId = :batchId AND s.status = 'COMPLETED'")
    Long sumCompletedNetAmountByBatchId(@Param("batchId") UUID batchId);

    long countByStatus(SettlementStatus status);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s")
    Long sumAllNetAmount();

    @Query("SELECT COALESCE(SUM(s.grossAmount), 0) FROM Settlement s")
    Long sumAllGrossAmount();

    @Query("SELECT COALESCE(SUM(s.fees), 0) FROM Settlement s")
    Long sumAllFees();

    @Query("SELECT COUNT(DISTINCT s.merchantId) FROM Settlement s")
    int countDistinctMerchants();
}
