package com.fdbpay.corporate.service.impl;

import com.fdbpay.corporate.dto.request.ApproveRequest;
import com.fdbpay.corporate.dto.request.RejectRequest;
import com.fdbpay.corporate.dto.request.SubmitForApprovalRequest;
import com.fdbpay.corporate.dto.response.ApprovalWorkflowResponse;
import com.fdbpay.corporate.model.ApprovalWorkflow;
import com.fdbpay.corporate.model.BulkDisbursement;
import com.fdbpay.corporate.repository.ApprovalWorkflowRepository;
import com.fdbpay.corporate.repository.BulkDisbursementRepository;
import com.fdbpay.corporate.service.ApprovalService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final BulkDisbursementRepository bulkDisbursementRepository;

    @Override
    @Transactional
    public ApiResponse<ApprovalWorkflowResponse> submitForApproval(UUID userId, SubmitForApprovalRequest request) {
        BulkDisbursement disbursement = bulkDisbursementRepository.findById(request.getBulkDisbursementId())
                .orElseThrow(() -> new ResourceNotFoundException("Bulk disbursement", request.getBulkDisbursementId().toString()));

        List<ApprovalWorkflow> existingApprovals = approvalWorkflowRepository
                .findByBulkDisbursementId(request.getBulkDisbursementId());
        boolean alreadySubmitted = existingApprovals.stream()
                .anyMatch(a -> "PENDING".equals(a.getStatus()));
        if (alreadySubmitted) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "This disbursement is already submitted for approval");
        }

        UUID approverId = findAlternativeApprover(userId, disbursement.getCorporateUserId());

        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .id(UUID.randomUUID())
                .bulkDisbursementId(request.getBulkDisbursementId())
                .approverUserId(approverId)
                .status("PENDING")
                .build();
        approvalWorkflowRepository.save(workflow);

        log.info("Approval workflow created: id={}, disbursementId={}, submitter={}, approver={}",
                workflow.getId(), request.getBulkDisbursementId(), userId, approverId);

        return ApiResponse.success(mapToResponse(workflow));
    }

    @Override
    @Transactional
    public ApiResponse<ApprovalWorkflowResponse> approve(UUID workflowId, UUID approverId, ApproveRequest request) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval workflow", workflowId.toString()));

        if ("APPROVED".equals(workflow.getStatus()) || "REJECTED".equals(workflow.getStatus())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "This approval workflow has already been processed");
        }

        BulkDisbursement disbursement = bulkDisbursementRepository.findById(workflow.getBulkDisbursementId())
                .orElseThrow(() -> new ResourceNotFoundException("Bulk disbursement", workflow.getBulkDisbursementId().toString()));

        if (disbursement.getCorporateUserId().equals(approverId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Approvers cannot approve their own submission (maker-checker violation)");
        }

        workflow.setStatus("APPROVED");
        workflow.setComments(request.getComments());
        workflow.setApprovedAt(OffsetDateTime.now());
        approvalWorkflowRepository.save(workflow);

        List<ApprovalWorkflow> allApprovals = approvalWorkflowRepository
                .findByBulkDisbursementId(workflow.getBulkDisbursementId());
        boolean allApproved = allApprovals.stream()
                .allMatch(a -> "APPROVED".equals(a.getStatus()));

        if (allApproved) {
            disbursement.setStatus("APPROVED");
            disbursement.setCompletedAt(OffsetDateTime.now());
            bulkDisbursementRepository.save(disbursement);
        }

        log.info("Approval completed: workflowId={}, approverId={}, allApproved={}", workflowId, approverId, allApproved);

        return ApiResponse.success(mapToResponse(workflow));
    }

    @Override
    @Transactional
    public ApiResponse<ApprovalWorkflowResponse> reject(UUID workflowId, UUID approverId, RejectRequest request) {
        ApprovalWorkflow workflow = approvalWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval workflow", workflowId.toString()));

        if ("APPROVED".equals(workflow.getStatus()) || "REJECTED".equals(workflow.getStatus())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "This approval workflow has already been processed");
        }

        workflow.setStatus("REJECTED");
        workflow.setComments(request.getReason());
        workflow.setApprovedAt(OffsetDateTime.now());
        approvalWorkflowRepository.save(workflow);

        BulkDisbursement disbursement = bulkDisbursementRepository.findById(workflow.getBulkDisbursementId())
                .orElseThrow(() -> new ResourceNotFoundException("Bulk disbursement", workflow.getBulkDisbursementId().toString()));
        disbursement.setStatus("REJECTED");
        disbursement.setCompletedAt(OffsetDateTime.now());
        bulkDisbursementRepository.save(disbursement);

        log.info("Rejection completed: workflowId={}, approverId={}", workflowId, approverId);

        return ApiResponse.success(mapToResponse(workflow));
    }

    @Override
    public ApiResponse<?> getPendingApprovals(UUID approverId, Pageable pageable) {
        Page<ApprovalWorkflow> pending = approvalWorkflowRepository
                .findByApproverUserIdAndStatusOrderByCreatedAtDesc(approverId, "PENDING", pageable);

        var response = pending.map(this::mapToResponse);

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
    public ApiResponse<List<ApprovalWorkflowResponse>> getApprovalHistory(UUID bulkDisbursementId) {
        List<ApprovalWorkflow> workflows = approvalWorkflowRepository.findByBulkDisbursementId(bulkDisbursementId);
        List<ApprovalWorkflowResponse> responses = workflows.stream()
                .map(this::mapToResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    private UUID findAlternativeApprover(UUID submitterId, UUID corporateUserId) {
        return UUID.randomUUID();
    }

    private ApprovalWorkflowResponse mapToResponse(ApprovalWorkflow workflow) {
        return ApprovalWorkflowResponse.builder()
                .id(workflow.getId())
                .bulkDisbursementId(workflow.getBulkDisbursementId())
                .approverUserId(workflow.getApproverUserId())
                .status(workflow.getStatus())
                .comments(workflow.getComments())
                .approvedAt(workflow.getApprovedAt())
                .createdAt(workflow.getCreatedAt())
                .build();
    }
}
