package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.ApprovalRequestResponse;
import com.fdbpay.merchant.service.model.ApprovalRequest;
import com.fdbpay.merchant.service.model.enums.ApprovalStatus;
import com.fdbpay.merchant.service.model.enums.ApprovalType;
import com.fdbpay.merchant.service.repository.ApprovalRequestRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final MerchantRepository merchantRepository;
    private final RefundService refundService;
    private final PayoutService payoutService;
    private final AuditService auditService;

    public List<ApprovalRequestResponse> listApprovals(UUID merchantId, ApprovalStatus status) {
        requireMerchant(merchantId);
        List<ApprovalRequest> requests = status == null
                ? approvalRequestRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                : approvalRequestRepository.findByMerchantIdAndStatusOrderByCreatedAtDesc(merchantId, status);
        return requests.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ApprovalRequestResponse approve(UUID merchantId, UUID approvalId, String reviewer) {
        ApprovalRequest request = getOwned(merchantId, approvalId);
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Approval request already resolved");
        }
        if (request.getType() == ApprovalType.REFUND) {
            refundService.approveRefund(merchantId, request.getRefId(), reviewer);
        } else if (request.getType() == ApprovalType.PAYOUT) {
            payoutService.approvePayout(merchantId, request.getRefId(), reviewer);
        }
        request.setStatus(ApprovalStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(OffsetDateTime.now());
        request = approvalRequestRepository.save(request);
        auditService.log(merchantId, "OWNER", null, null, "APPROVE", request.getType().name(),
                request.getId().toString(), request.getType() + " approval granted by " + reviewer);
        return mapToResponse(request);
    }

    @Transactional
    public ApprovalRequestResponse reject(UUID merchantId, UUID approvalId, String reviewer) {
        ApprovalRequest request = getOwned(merchantId, approvalId);
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Approval request already resolved");
        }
        if (request.getType() == ApprovalType.REFUND) {
            refundService.rejectRefund(merchantId, request.getRefId(), reviewer);
        } else if (request.getType() == ApprovalType.PAYOUT) {
            payoutService.rejectPayout(merchantId, request.getRefId(), reviewer);
        }
        request.setStatus(ApprovalStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(OffsetDateTime.now());
        request = approvalRequestRepository.save(request);
        auditService.log(merchantId, "OWNER", null, null, "REJECT", request.getType().name(),
                request.getId().toString(), request.getType() + " approval rejected by " + reviewer);
        return mapToResponse(request);
    }

    private ApprovalRequest getOwned(UUID merchantId, UUID approvalId) {
        ApprovalRequest request = approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", approvalId.toString()));
        if (!request.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Approval request does not belong to this merchant");
        }
        return request;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private ApprovalRequestResponse mapToResponse(ApprovalRequest request) {
        return ApprovalRequestResponse.builder()
                .id(request.getId())
                .type(request.getType())
                .amount(request.getAmount())
                .refId(request.getRefId())
                .initiatorName(request.getInitiatorName())
                .status(request.getStatus())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
