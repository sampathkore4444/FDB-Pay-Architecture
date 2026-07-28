package com.fdbpay.corporate.service;

import com.fdbpay.corporate.dto.request.ApproveRequest;
import com.fdbpay.corporate.dto.request.RejectRequest;
import com.fdbpay.corporate.dto.request.SubmitForApprovalRequest;
import com.fdbpay.corporate.dto.response.ApprovalWorkflowResponse;
import com.fdbpay.shared.dto.ApiResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ApprovalService {

    ApiResponse<ApprovalWorkflowResponse> submitForApproval(UUID userId, SubmitForApprovalRequest request);

    ApiResponse<ApprovalWorkflowResponse> approve(UUID workflowId, UUID approverId, ApproveRequest request);

    ApiResponse<ApprovalWorkflowResponse> reject(UUID workflowId, UUID approverId, RejectRequest request);

    ApiResponse<?> getPendingApprovals(UUID approverId, Pageable pageable);

    ApiResponse<List<ApprovalWorkflowResponse>> getApprovalHistory(UUID bulkDisbursementId);
}
