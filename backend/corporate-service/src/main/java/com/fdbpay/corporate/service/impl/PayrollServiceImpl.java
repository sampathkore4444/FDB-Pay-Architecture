package com.fdbpay.corporate.service.impl;

import com.fdbpay.corporate.dto.request.CreatePayrollRunRequest;
import com.fdbpay.corporate.dto.response.PayrollEmployeeResponse;
import com.fdbpay.corporate.dto.response.PayrollRunResponse;
import com.fdbpay.corporate.model.PayrollEmployee;
import com.fdbpay.corporate.model.PayrollRun;
import com.fdbpay.corporate.repository.PayrollEmployeeRepository;
import com.fdbpay.corporate.repository.PayrollRunRepository;
import com.fdbpay.corporate.service.PayrollService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollEmployeeRepository payrollEmployeeRepository;
    private final WebClient walletWebClient;

    @Override
    @Transactional
    public ApiResponse<PayrollRunResponse> createPayrollRun(UUID corporateUserId, CreatePayrollRunRequest request) {
        long totalAmount = request.getEmployees().stream()
                .mapToLong(CreatePayrollRunRequest.PayrollEmployeeItem::getAmount)
                .sum();

        PayrollRun payrollRun = PayrollRun.builder()
                .id(UUID.randomUUID())
                .corporateUserId(corporateUserId)
                .period(request.getPeriod())
                .totalEmployees(request.getEmployees().size())
                .totalAmount(totalAmount)
                .status("DRAFT")
                .build();
        payrollRunRepository.save(payrollRun);

        for (CreatePayrollRunRequest.PayrollEmployeeItem item : request.getEmployees()) {
            PayrollEmployee employee = PayrollEmployee.builder()
                    .id(UUID.randomUUID())
                    .payrollRunId(payrollRun.getId())
                    .employeeId(item.getEmployeeId())
                    .employeeName(item.getEmployeeName())
                    .phone(item.getPhone())
                    .amount(item.getAmount())
                    .status("PENDING")
                    .build();
            payrollEmployeeRepository.save(employee);
        }

        log.info("Payroll run created: id={}, user={}, period={}, employees={}, totalAmount={}",
                payrollRun.getId(), corporateUserId, request.getPeriod(), request.getEmployees().size(), totalAmount);

        PayrollRunResponse response = mapToResponse(payrollRun);
        response.setEmployees(getEmployeeResponses(payrollRun.getId()));
        return ApiResponse.success(response);
    }

    @Override
    @Transactional
    public ApiResponse<PayrollRunResponse> submitPayroll(UUID payrollRunId, UUID userId) {
        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run", payrollRunId.toString()));

        if (!"DRAFT".equals(payrollRun.getStatus())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Payroll run must be in DRAFT status to submit. Current status: " + payrollRun.getStatus());
        }

        payrollRun.setStatus("SUBMITTED");
        payrollRun.setSubmittedBy(userId);
        payrollRunRepository.save(payrollRun);

        log.info("Payroll run submitted: id={}, submittedBy={}", payrollRunId, userId);

        PayrollRunResponse response = mapToResponse(payrollRun);
        response.setEmployees(getEmployeeResponses(payrollRunId));
        return ApiResponse.success(response);
    }

    @Override
    @Transactional
    public ApiResponse<PayrollRunResponse> approvePayroll(UUID payrollRunId, UUID approverId) {
        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run", payrollRunId.toString()));

        if (!"SUBMITTED".equals(payrollRun.getStatus())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Payroll run must be in SUBMITTED status to approve. Current status: " + payrollRun.getStatus());
        }

        if (payrollRun.getSubmittedBy() != null && payrollRun.getSubmittedBy().equals(approverId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Approvers cannot approve their own submission (maker-checker violation)");
        }

        payrollRun.setStatus("APPROVED");
        payrollRun.setApprovedBy(approverId);
        payrollRunRepository.save(payrollRun);

        payrollRun.setStatus("PROCESSING");
        payrollRunRepository.save(payrollRun);

        List<PayrollEmployee> employees = payrollEmployeeRepository.findByPayrollRunId(payrollRunId);
        int successCount = 0;
        int failCount = 0;

        for (PayrollEmployee employee : employees) {
            try {
                Map<String, Object> creditRequest = Map.of(
                        "amount", employee.getAmount(),
                        "idempotencyKey", "PAYROLL_" + payrollRunId + "_" + employee.getId()
                );

                walletWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/wallet/credit")
                                .queryParam("userId", employee.getId())
                                .build())
                        .bodyValue(creditRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                employee.setStatus("PAID");
                employee.setTransactionRef("TXN_" + UUID.randomUUID());
                payrollEmployeeRepository.save(employee);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to credit employee {}: {}", employee.getEmployeeId(), e.getMessage());
                employee.setStatus("FAILED");
                payrollEmployeeRepository.save(employee);
                failCount++;
            }
        }

        payrollRun.setStatus(failCount == 0 ? "COMPLETED" : (successCount == 0 ? "FAILED" : "COMPLETED"));
        payrollRun.setCompletedAt(OffsetDateTime.now());
        payrollRunRepository.save(payrollRun);

        log.info("Payroll run processing completed: id={}, success={}, failed={}", payrollRunId, successCount, failCount);

        PayrollRunResponse response = mapToResponse(payrollRun);
        response.setEmployees(getEmployeeResponses(payrollRunId));
        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<PayrollRunResponse> getPayrollRun(UUID payrollRunId) {
        PayrollRun payrollRun = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run", payrollRunId.toString()));

        PayrollRunResponse response = mapToResponse(payrollRun);
        response.setEmployees(getEmployeeResponses(payrollRunId));
        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<?> getPayrollHistory(UUID corporateUserId, Pageable pageable) {
        Page<PayrollRun> runs = payrollRunRepository.findByCorporateUserIdOrderByCreatedAtDesc(corporateUserId, pageable);

        var response = runs.map(this::mapToResponse);

        ApiResponse.Pagination pagination = ApiResponse.Pagination.builder()
                .page(response.getNumber())
                .perPage(response.getSize())
                .total(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .build();

        ApiResponse.Meta meta = ApiResponse.Meta.builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now().toString())
                .pagination(pagination)
                .build();

        return ApiResponse.<Object>builder()
                .success(true)
                .data(response.getContent())
                .meta(meta)
                .build();
    }

    @Override
    public ApiResponse<List<PayrollEmployeeResponse>> getPayrollEmployees(UUID payrollRunId) {
        payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run", payrollRunId.toString()));

        List<PayrollEmployeeResponse> employees = getEmployeeResponses(payrollRunId);
        return ApiResponse.success(employees);
    }

    private List<PayrollEmployeeResponse> getEmployeeResponses(UUID payrollRunId) {
        return payrollEmployeeRepository.findByPayrollRunId(payrollRunId).stream()
                .map(this::mapToEmployeeResponse)
                .toList();
    }

    private PayrollRunResponse mapToResponse(PayrollRun run) {
        return PayrollRunResponse.builder()
                .id(run.getId())
                .corporateUserId(run.getCorporateUserId())
                .period(run.getPeriod())
                .totalEmployees(run.getTotalEmployees())
                .totalAmount(run.getTotalAmount())
                .status(run.getStatus())
                .submittedBy(run.getSubmittedBy())
                .approvedBy(run.getApprovedBy())
                .createdAt(run.getCreatedAt())
                .completedAt(run.getCompletedAt())
                .build();
    }

    private PayrollEmployeeResponse mapToEmployeeResponse(PayrollEmployee employee) {
        return PayrollEmployeeResponse.builder()
                .id(employee.getId())
                .payrollRunId(employee.getPayrollRunId())
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getEmployeeName())
                .phone(employee.getPhone())
                .amount(employee.getAmount())
                .status(employee.getStatus())
                .transactionRef(employee.getTransactionRef())
                .build();
    }
}
