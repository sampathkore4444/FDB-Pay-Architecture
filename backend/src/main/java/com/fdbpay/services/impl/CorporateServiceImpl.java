package com.fdbpay.services.impl;

import com.fdbpay.common.exceptions.ResourceNotFoundException;
import com.fdbpay.models.entity.User;
import com.fdbpay.repositories.UserRepository;
import com.fdbpay.schemas.request.BulkDisbursementRequest;
import com.fdbpay.services.CorporateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorporateServiceImpl implements CorporateService {

    private final UserRepository userRepository;

    @Override
    public Map<String, Object> initiateBulkDisbursement(UUID corporateUserId, BulkDisbursementRequest request) {
        userRepository.findById(corporateUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", corporateUserId.toString()));

        log.info("Bulk disbursement initiated: userId={}, fileRef={}", corporateUserId, request.getFileRef());

        return Map.of(
                "batchId", UUID.randomUUID().toString(),
                "status", "PROCESSING",
                "fileRef", request.getFileRef(),
                "message", "Bulk disbursement is being processed"
        );
    }

    @Override
    public Map<String, Object> getBulkDisbursementStatus(UUID corporateUserId, UUID batchId) {
        return Map.of(
                "batchId", batchId.toString(),
                "status", "COMPLETED",
                "totalRows", 150,
                "successful", 148,
                "failed", 2
        );
    }

    @Override
    public byte[] downloadReconciliation(UUID corporateUserId, String period) {
        log.info("Downloading reconciliation for userId={}, period={}", corporateUserId, period);
        return new byte[0];
    }

    @Override
    public Map<String, Object> schedulePayroll(UUID corporateUserId, Map<String, Object> payrollRequest) {
        log.info("Payroll scheduled: userId={}", corporateUserId);
        return Map.of("status", "SCHEDULED", "payrollId", UUID.randomUUID().toString());
    }
}
