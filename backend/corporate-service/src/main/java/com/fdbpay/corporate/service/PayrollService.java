package com.fdbpay.corporate.service;

import com.fdbpay.corporate.dto.request.CreatePayrollRunRequest;
import com.fdbpay.corporate.dto.response.PayrollEmployeeResponse;
import com.fdbpay.corporate.dto.response.PayrollRunResponse;
import com.fdbpay.shared.dto.ApiResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PayrollService {

    ApiResponse<PayrollRunResponse> createPayrollRun(UUID corporateUserId, CreatePayrollRunRequest request);

    ApiResponse<PayrollRunResponse> submitPayroll(UUID payrollRunId, UUID userId);

    ApiResponse<PayrollRunResponse> approvePayroll(UUID payrollRunId, UUID approverId);

    ApiResponse<PayrollRunResponse> getPayrollRun(UUID payrollRunId);

    ApiResponse<?> getPayrollHistory(UUID corporateUserId, Pageable pageable);

    ApiResponse<List<PayrollEmployeeResponse>> getPayrollEmployees(UUID payrollRunId);
}
