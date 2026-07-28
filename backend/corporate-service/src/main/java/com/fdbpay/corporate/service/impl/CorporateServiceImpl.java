package com.fdbpay.corporate.service.impl;

import com.fdbpay.corporate.dto.request.BulkDisbursementRequest;
import com.fdbpay.corporate.dto.request.PayrollScheduleRequest;
import com.fdbpay.corporate.dto.response.BulkDisbursementResponse;
import com.fdbpay.corporate.dto.response.PayrollScheduleResponse;
import com.fdbpay.corporate.model.BulkDisbursement;
import com.fdbpay.corporate.model.PayrollSchedule;
import com.fdbpay.corporate.repository.BulkDisbursementRepository;
import com.fdbpay.corporate.repository.PayrollScheduleRepository;
import com.fdbpay.corporate.service.CorporateService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.event.TransactionEvent;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorporateServiceImpl implements CorporateService {

    private final BulkDisbursementRepository bulkDisbursementRepository;
    private final PayrollScheduleRepository payrollScheduleRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Override
    @Transactional
    public ApiResponse<BulkDisbursementResponse> initiateBulkDisbursement(UUID userId, BulkDisbursementRequest request) {
        BulkDisbursement disbursement = BulkDisbursement.builder()
                .id(UUID.randomUUID())
                .corporateUserId(userId)
                .fileRef(request.getFileRef())
                .description(request.getDescription())
                .status("PENDING")
                .build();
        bulkDisbursementRepository.save(disbursement);

        TransactionEvent event = TransactionEvent.builder()
                .transactionId(disbursement.getId())
                .type("BULK_DISBURSEMENT")
                .status("PENDING")
                .senderUserId(userId)
                .timestamp(OffsetDateTime.now())
                .metadata(Map.of(
                        "fileRef", request.getFileRef(),
                        "idempotencyKey", request.getIdempotencyKey(),
                        "description", request.getDescription() != null ? request.getDescription() : ""
                ))
                .build();
        kafkaTemplate.send("bulk.disbursement.initiated", event);

        log.info("Bulk disbursement initiated: id={}, user={}, fileRef={}", disbursement.getId(), userId, request.getFileRef());

        return ApiResponse.success(mapToBulkDisbursementResponse(disbursement));
    }

    @Override
    public ApiResponse<BulkDisbursementResponse> getStatus(UUID userId, UUID batchId) {
        BulkDisbursement disbursement = bulkDisbursementRepository.findByIdAndCorporateUserId(batchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulk disbursement", batchId.toString()));
        return ApiResponse.success(mapToBulkDisbursementResponse(disbursement));
    }

    @Override
    public ApiResponse<?> downloadReconciliation(UUID userId, String period) {
        log.info("Reconciliation download requested: user={}, period={}", userId, period);

        Map<String, Object> reconciliationData = Map.of(
                "userId", userId.toString(),
                "period", period,
                "downloadUrl", "/api/corp/reconciliation/" + userId + "/" + period + ".csv",
                "generatedAt", OffsetDateTime.now().toString()
        );

        return ApiResponse.success(reconciliationData);
    }

    @Override
    @Transactional
    public ApiResponse<PayrollScheduleResponse> schedulePayroll(UUID userId, PayrollScheduleRequest request) {
        PayrollSchedule schedule = PayrollSchedule.builder()
                .id(UUID.randomUUID())
                .corporateUserId(userId)
                .scheduledDate(request.getScheduledDate())
                .status("PENDING")
                .build();
        payrollScheduleRepository.save(schedule);

        log.info("Payroll scheduled: id={}, user={}, date={}", schedule.getId(), userId, request.getScheduledDate());

        return ApiResponse.success(mapToPayrollScheduleResponse(schedule));
    }

    private BulkDisbursementResponse mapToBulkDisbursementResponse(BulkDisbursement disbursement) {
        return BulkDisbursementResponse.builder()
                .id(disbursement.getId())
                .status(disbursement.getStatus())
                .totalRows(disbursement.getTotalRows())
                .successfulRows(disbursement.getSuccessfulRows())
                .failedRows(disbursement.getFailedRows())
                .createdAt(disbursement.getCreatedAt())
                .build();
    }

    private PayrollScheduleResponse mapToPayrollScheduleResponse(PayrollSchedule schedule) {
        return PayrollScheduleResponse.builder()
                .id(schedule.getId())
                .scheduledDate(schedule.getScheduledDate())
                .status(schedule.getStatus())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}
