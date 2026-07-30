package com.fdbpay.settlement.service.service.impl;

import com.fdbpay.settlement.service.dto.request.SettlementQueryRequest;
import com.fdbpay.settlement.service.dto.request.TriggerSettlementRequest;
import com.fdbpay.settlement.service.dto.response.SettlementBatchResponse;
import com.fdbpay.settlement.service.dto.response.SettlementResponse;
import com.fdbpay.settlement.service.dto.response.SettlementSummaryResponse;
import com.fdbpay.settlement.service.model.Settlement;
import com.fdbpay.settlement.service.model.SettlementBatch;
import com.fdbpay.settlement.service.model.enums.BatchStatus;
import com.fdbpay.settlement.service.model.enums.SettlementStatus;
import com.fdbpay.settlement.service.repository.SettlementBatchRepository;
import com.fdbpay.settlement.service.repository.SettlementRepository;
import com.fdbpay.settlement.service.service.SettlementService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.shared.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementBatchRepository batchRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${settlement.fee-percentage:1.5}")
    private double feePercentage;

    private static final String DAILY_HASH_PREFIX = "merchant:daily:";

    @Override
    @Scheduled(cron = "${settlement.cron}")
    @Transactional
    public void triggerDailySettlement() {
        log.info("Starting daily settlement job");

        LocalDate today = LocalDate.now();
        SettlementBatch batch = createOrGetBatch(today);

        if (batch.getStatus() == BatchStatus.COMPLETED) {
            log.warn("Batch for {} already completed, skipping", today);
            return;
        }

        batch.setStatus(BatchStatus.PROCESSING);
        batch.setStartedAt(OffsetDateTime.now());
        batch = batchRepository.save(batch);

        try {
            Set<String> merchantKeys = redisTemplate.keys(DAILY_HASH_PREFIX + today + ":*");
            if (merchantKeys == null || merchantKeys.isEmpty()) {
                log.info("No merchant transactions found for {}", today);
                batch.setStatus(BatchStatus.COMPLETED);
                batch.setTotalMerchants(0);
                batch.setTotalGrossAmount(0L);
                batch.setTotalFees(0L);
                batch.setTotalNetAmount(0L);
                batch.setCompletedAt(OffsetDateTime.now());
                batchRepository.save(batch);
                return;
            }

            int merchantCount = 0;
            long totalGross = 0;
            long totalFees = 0;
            long totalNet = 0;

            OffsetDateTime periodStart = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            OffsetDateTime periodEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

            for (String key : merchantKeys) {
                String merchantIdStr = key.replace(DAILY_HASH_PREFIX + today + ":", "");
                UUID merchantId = UUID.fromString(merchantIdStr);

                HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
                Object grossObj = hashOps.get(key, "grossAmount");
                Object countObj = hashOps.get(key, "transactionCount");

                long grossAmount = grossObj != null ? Long.parseLong(grossObj.toString()) : 0L;
                int transactionCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

                if (grossAmount <= 0) {
                    continue;
                }

                long fees = Math.round(grossAmount * feePercentage / 100.0);
                long netAmount = grossAmount - fees;

                Settlement settlement = Settlement.builder()
                        .merchantId(merchantId)
                        .batchId(batch.getId())
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .grossAmount(grossAmount)
                        .fees(fees)
                        .netAmount(netAmount)
                        .status(SettlementStatus.COMPLETED)
                        .settlementRef(IdGenerator.generateSettlementRef())
                        .transactionCount(transactionCount)
                        .settledAt(OffsetDateTime.now())
                        .createdAt(OffsetDateTime.now())
                        .build();

                settlementRepository.save(settlement);
                publishSettlementEvent(settlement);

                merchantCount++;
                totalGross += grossAmount;
                totalFees += fees;
                totalNet += netAmount;

                redisTemplate.delete(key);
            }

            batch.setTotalMerchants(merchantCount);
            batch.setTotalGrossAmount(totalGross);
            batch.setTotalFees(totalFees);
            batch.setTotalNetAmount(totalNet);
            batch.setStatus(BatchStatus.COMPLETED);
            batch.setCompletedAt(OffsetDateTime.now());
            batchRepository.save(batch);

            log.info("Daily settlement completed: batchId={}, merchants={}, gross={}, fees={}, net={}",
                    batch.getId(), merchantCount, totalGross, totalFees, totalNet);

        } catch (Exception e) {
            log.error("Daily settlement failed for batch {}", batch.getId(), e);
            batch.setStatus(BatchStatus.FAILED);
            batchRepository.save(batch);
        }
    }

    @Override
    @Transactional
    public SettlementResponse triggerMerchantSettlement(TriggerSettlementRequest request) {
        if (request.getMerchantId() == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "merchantId is required for manual trigger");
        }

        UUID merchantId = request.getMerchantId();
        LocalDate today = LocalDate.now();
        OffsetDateTime periodStart = today.minusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime periodEnd = today.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        List<Settlement> existing = settlementRepository.findByMerchantIdAndPeriodBetween(merchantId, periodStart, periodEnd);
        if (!existing.isEmpty()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Settlement already exists for merchant " + merchantId + " in the current period");
        }

        String hashKey = DAILY_HASH_PREFIX + today.minusDays(1) + ":" + merchantId;
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
        Object grossObj = hashOps.get(hashKey, "grossAmount");
        Object countObj = hashOps.get(hashKey, "transactionCount");

        long grossAmount = grossObj != null ? Long.parseLong(grossObj.toString()) : 0L;
        int transactionCount = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        if (grossAmount <= 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "No transactions found for merchant " + merchantId + " in the settlement period");
        }

        long fees = Math.round(grossAmount * feePercentage / 100.0);
        long netAmount = grossAmount - fees;

        SettlementBatch batch = createOrGetBatch(today.minusDays(1));

        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .batchId(batch.getId())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .grossAmount(grossAmount)
                .fees(fees)
                .netAmount(netAmount)
                .status(SettlementStatus.COMPLETED)
                .settlementRef(IdGenerator.generateSettlementRef())
                .transactionCount(transactionCount)
                .settledAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        settlement = settlementRepository.save(settlement);
        publishSettlementEvent(settlement);

        redisTemplate.delete(hashKey);

        log.info("Manual settlement triggered for merchant: merchantId={}, net={}", merchantId, netAmount);

        return mapToResponse(settlement);
    }

    @Override
    public SettlementResponse getSettlement(UUID settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", settlementId.toString()));
        return mapToResponse(settlement);
    }

    @Override
    public Page<SettlementResponse> getMerchantSettlements(UUID merchantId, int page, int size) {
        Page<Settlement> settlements = settlementRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(page, size));
        return settlements.map(this::mapToResponse);
    }

    @Override
    public SettlementSummaryResponse getSettlementSummary(UUID batchId) {
        batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SettlementBatch", batchId.toString()));

        Long totalNet = settlementRepository.sumCompletedNetAmountByBatchId(batchId);
        Long totalFees = settlementRepository.sumFeesByBatchId(batchId);
        int merchantCount = settlementRepository.countDistinctMerchantsByBatchId(batchId);

        return SettlementSummaryResponse.builder()
                .totalSettled(totalNet)
                .totalFees(totalFees)
                .merchantCount(merchantCount)
                .batchId(batchId)
                .build();
    }

    @Override
    public Map<String, Object> getOverallSummary() {
        long pending = settlementRepository.countByStatus(SettlementStatus.PENDING);
        long completed = settlementRepository.countByStatus(SettlementStatus.COMPLETED);
        long failed = settlementRepository.countByStatus(SettlementStatus.FAILED);
        Long totalNet = settlementRepository.sumAllNetAmount();
        Long totalFees = settlementRepository.sumAllFees();
        Long totalGross = settlementRepository.sumAllGrossAmount();
        int merchantCount = settlementRepository.countDistinctMerchants();

        return Map.of(
                "pending", pending,
                "completed", completed,
                "failed", failed,
                "totalGrossAmount", totalGross,
                "totalFees", totalFees,
                "totalNetAmount", totalNet,
                "merchantCount", merchantCount
        );
    }

    @Override
    @Transactional
    public SettlementBatchResponse reconcile() {
        log.info("Starting settlement reconciliation");

        List<Settlement> pendingSettlements = settlementRepository.findByStatus(SettlementStatus.PENDING);
        int discrepancyCount = 0;

        for (Settlement settlement : pendingSettlements) {
            OffsetDateTime now = OffsetDateTime.now();
            long expectedFees = Math.round(settlement.getGrossAmount() * feePercentage / 100.0);
            long expectedNet = settlement.getGrossAmount() - expectedFees;

            if (settlement.getFees() != expectedFees || settlement.getNetAmount() != expectedNet) {
                log.warn("Discrepancy found for settlement {}: expected fees={}, actual fees={}, expected net={}, actual net={}",
                        settlement.getId(), expectedFees, settlement.getFees(), expectedNet, settlement.getNetAmount());

                settlement.setFees(expectedFees);
                settlement.setNetAmount(expectedNet);
                settlement.setStatus(SettlementStatus.FAILED);
                settlementRepository.save(settlement);
                discrepancyCount++;
            } else {
                settlement.setStatus(SettlementStatus.COMPLETED);
                settlement.setSettledAt(now);
                settlementRepository.save(settlement);
            }
        }

        if (!pendingSettlements.isEmpty()) {
            SettlementBatch batch = SettlementBatch.builder()
                    .batchDate(LocalDate.now())
                    .totalMerchants(pendingSettlements.size())
                    .totalGrossAmount(pendingSettlements.stream().mapToLong(Settlement::getGrossAmount).sum())
                    .totalFees(pendingSettlements.stream().mapToLong(Settlement::getFees).sum())
                    .totalNetAmount(pendingSettlements.stream().mapToLong(Settlement::getNetAmount).sum())
                    .status(discrepancyCount > 0 ? BatchStatus.FAILED : BatchStatus.COMPLETED)
                    .startedAt(OffsetDateTime.now())
                    .completedAt(OffsetDateTime.now())
                    .build();
            batch = batchRepository.save(batch);

            log.info("Reconciliation completed: discrepancies={}, batchId={}", discrepancyCount, batch.getId());
            return mapToBatchResponse(batch);
        }

        log.info("Reconciliation completed: no pending settlements found");
        return SettlementBatchResponse.builder()
                .batchDate(LocalDate.now())
                .totalMerchants(0)
                .totalGrossAmount(0L)
                .totalFees(0L)
                .totalNetAmount(0L)
                .status(BatchStatus.COMPLETED)
                .build();
    }

    private SettlementBatch createOrGetBatch(LocalDate date) {
        return batchRepository.findByBatchDate(date)
                .orElseGet(() -> {
                    SettlementBatch batch = SettlementBatch.builder()
                            .batchDate(date)
                            .totalMerchants(0)
                            .totalGrossAmount(0L)
                            .totalFees(0L)
                            .totalNetAmount(0L)
                            .status(BatchStatus.PENDING)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    return batchRepository.save(batch);
                });
    }

    private void publishSettlementEvent(Settlement settlement) {
        try {
            Map<String, Object> event = Map.of(
                    "settlementId", settlement.getId().toString(),
                    "merchantId", settlement.getMerchantId().toString(),
                    "batchId", settlement.getBatchId().toString(),
                    "netAmount", settlement.getNetAmount(),
                    "fees", settlement.getFees(),
                    "settlementRef", settlement.getSettlementRef(),
                    "timestamp", OffsetDateTime.now().toString()
            );
            kafkaTemplate.send("settlement.completed", settlement.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish settlement event: settlementId={}", settlement.getId(), e);
        }
    }

    private SettlementResponse mapToResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .merchantId(settlement.getMerchantId())
                .batchId(settlement.getBatchId())
                .periodStart(settlement.getPeriodStart())
                .periodEnd(settlement.getPeriodEnd())
                .grossAmount(settlement.getGrossAmount())
                .fees(settlement.getFees())
                .netAmount(settlement.getNetAmount())
                .status(settlement.getStatus())
                .settledAt(settlement.getSettledAt())
                .settlementRef(settlement.getSettlementRef())
                .transactionCount(settlement.getTransactionCount())
                .createdAt(settlement.getCreatedAt())
                .build();
    }

    private SettlementBatchResponse mapToBatchResponse(SettlementBatch batch) {
        return SettlementBatchResponse.builder()
                .id(batch.getId())
                .batchDate(batch.getBatchDate())
                .totalMerchants(batch.getTotalMerchants())
                .totalGrossAmount(batch.getTotalGrossAmount())
                .totalFees(batch.getTotalFees())
                .totalNetAmount(batch.getTotalNetAmount())
                .status(batch.getStatus())
                .startedAt(batch.getStartedAt())
                .completedAt(batch.getCompletedAt())
                .createdAt(batch.getCreatedAt())
                .build();
    }
}
