package com.fdbpay.corporate.service;

import com.fdbpay.corporate.dto.request.BulkDisbursementRequest;
import com.fdbpay.corporate.dto.request.PayrollScheduleRequest;
import com.fdbpay.corporate.dto.response.BulkDisbursementResponse;
import com.fdbpay.corporate.dto.response.PayrollScheduleResponse;
import com.fdbpay.shared.dto.ApiResponse;

import java.util.UUID;

public interface CorporateService {

    ApiResponse<BulkDisbursementResponse> initiateBulkDisbursement(UUID userId, BulkDisbursementRequest request);

    ApiResponse<BulkDisbursementResponse> getStatus(UUID userId, UUID batchId);

    ApiResponse<?> downloadReconciliation(UUID userId, String period);

    ApiResponse<PayrollScheduleResponse> schedulePayroll(UUID userId, PayrollScheduleRequest request);
}
