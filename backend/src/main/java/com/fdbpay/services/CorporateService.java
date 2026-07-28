package com.fdbpay.services;

import com.fdbpay.schemas.request.BulkDisbursementRequest;
import com.fdbpay.schemas.response.TransactionResponse;

import java.util.Map;
import java.util.UUID;

public interface CorporateService {

    Map<String, Object> initiateBulkDisbursement(UUID corporateUserId, BulkDisbursementRequest request);

    Map<String, Object> getBulkDisbursementStatus(UUID corporateUserId, UUID batchId);

    byte[] downloadReconciliation(UUID corporateUserId, String period);

    Map<String, Object> schedulePayroll(UUID corporateUserId, Map<String, Object> payrollRequest);
}
