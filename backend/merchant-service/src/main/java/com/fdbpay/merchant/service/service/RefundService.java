package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.RefundRequest;
import com.fdbpay.merchant.service.dto.response.RefundResponse;
import com.fdbpay.merchant.service.model.ApprovalRequest;
import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.Refund;
import com.fdbpay.merchant.service.model.enums.ApprovalStatus;
import com.fdbpay.merchant.service.model.enums.ApprovalType;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.model.enums.RefundStatus;
import com.fdbpay.merchant.service.repository.ApprovalRequestRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.merchant.service.repository.RefundRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;

    public List<RefundResponse> listRefunds(UUID merchantId, RefundStatus status) {
        requireMerchant(merchantId);
        List<Refund> refunds = status == null
                ? refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                : refundRepository.findByMerchantIdAndStatus(merchantId, status);
        return refunds.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public RefundResponse createRefund(UUID merchantId, RefundRequest request) {
        requireMerchant(merchantId);
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId().toString()));
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Order does not belong to this merchant");
        }
        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Cannot refund a non-paid order");
        }
        long refundable = order.getTotal() - order.getRefundAmount();
        if (request.getAmount() > refundable) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Refund amount exceeds refundable balance");
        }

        Refund refund = Refund.builder()
                .merchantId(merchantId)
                .orderId(order.getId())
                .customerPhone(order.getCustomerPhone())
                .amount(request.getAmount())
                .reason(request.getReason())
                .status(request.isRequireApproval() ? RefundStatus.PENDING : RefundStatus.COMPLETED)
                .build();
        refund = refundRepository.save(refund);

        if (request.isRequireApproval()) {
            approvalRequestRepository.save(ApprovalRequest.builder()
                    .merchantId(merchantId)
                    .type(ApprovalType.REFUND)
                    .amount(request.getAmount())
                    .refId(refund.getId())
                    .status(ApprovalStatus.PENDING)
                    .build());
        } else {
            applyRefund(order, request.getAmount());
        }
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "REFUND", refund.getId().toString(),
                "Refund of " + request.getAmount() + " MMK on order " + order.getId());
        return mapToResponse(refund);
    }

    @Transactional
    public void applyRefund(Order order, long amount) {
        order.setRefundAmount(order.getRefundAmount() + amount);
        if (order.getRefundAmount() >= order.getTotal()) {
            order.setStatus(OrderStatus.REFUNDED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_REFUNDED);
        }
        orderRepository.save(order);
    }

    @Transactional
    public RefundResponse approveRefund(UUID merchantId, UUID refundId, String reviewer) {
        Refund refund = getOwned(merchantId, refundId);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Refund is not pending approval");
        }
        refund.setStatus(RefundStatus.COMPLETED);
        refund = refundRepository.save(refund);
        Order order = refund.getOrderId() == null ? null : orderRepository.findById(refund.getOrderId()).orElse(null);
        if (order != null) {
            applyRefund(order, refund.getAmount());
        }
        auditService.log(merchantId, "OWNER", null, null, "APPROVE", "REFUND", refundId.toString(),
                "Refund approved by " + reviewer);
        return mapToResponse(refund);
    }

    @Transactional
    public RefundResponse rejectRefund(UUID merchantId, UUID refundId, String reviewer) {
        Refund refund = getOwned(merchantId, refundId);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Refund is not pending approval");
        }
        refund.setStatus(RefundStatus.REJECTED);
        refund = refundRepository.save(refund);
        auditService.log(merchantId, "OWNER", null, null, "REJECT", "REFUND", refundId.toString(),
                "Refund rejected by " + reviewer);
        return mapToResponse(refund);
    }

    private Refund getOwned(UUID merchantId, UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId.toString()));
        if (!refund.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Refund does not belong to this merchant");
        }
        return refund;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private RefundResponse mapToResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .orderId(refund.getOrderId())
                .transactionId(refund.getTransactionId())
                .customerPhone(refund.getCustomerPhone())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .createdAt(refund.getCreatedAt())
                .build();
    }
}
