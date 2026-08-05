package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.dto.request.CreatePaymentLinkRequest;
import com.fdbpay.merchant.service.dto.response.PaymentLinkPublicResponse;
import com.fdbpay.merchant.service.dto.response.PaymentLinkResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.PaymentLink;
import com.fdbpay.merchant.service.model.enums.PaymentLinkStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.PaymentLinkRepository;
import com.fdbpay.merchant.service.service.PaymentLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLinkServiceImpl implements PaymentLinkService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private final PaymentLinkRepository paymentLinkRepository;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public PaymentLinkResponse create(UUID merchantId, CreatePaymentLinkRequest request) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        PaymentLink link = PaymentLink.builder()
                .merchantId(merchantId)
                .token(generateToken())
                .amount(request.getAmount())
                .description(request.getDescription())
                .customerPhone(request.getCustomerPhone())
                .customerName(request.getCustomerName())
                .status(PaymentLinkStatus.ACTIVE)
                .singleUse(true)
                .expiresAt(request.getExpiresAt())
                .build();

        link = paymentLinkRepository.save(link);
        log.info("Payment link created: linkId={}, merchantId={}, amount={}", link.getId(), merchantId, request.getAmount());
        return mapToResponse(link);
    }

    @Override
    public Page<PaymentLinkResponse> getByMerchant(UUID merchantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return paymentLinkRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageRequest)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PaymentLinkResponse deactivate(UUID merchantId, UUID linkId) {
        PaymentLink link = paymentLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentLink", linkId.toString()));
        if (!link.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Payment link does not belong to this merchant");
        }
        link.setStatus(PaymentLinkStatus.DEACTIVATED);
        link = paymentLinkRepository.save(link);
        log.info("Payment link deactivated: linkId={}", linkId);
        return mapToResponse(link);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentLinkPublicResponse getByToken(String token) {
        PaymentLink link = paymentLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentLink", "token=" + token));
        if (link.getStatus() != PaymentLinkStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Payment link is not active");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Payment link has expired");
        }
        return mapToPublic(link);
    }

    @Override
    @Transactional
    public PaymentLinkPublicResponse markPaid(String token) {
        PaymentLink link = paymentLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentLink", "token=" + token));
        if (link.getStatus() == PaymentLinkStatus.PAID) {
            throw new BusinessException(ErrorCodes.DUPLICATE_TRANSACTION, "Payment link already paid");
        }
        if (link.getStatus() != PaymentLinkStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Payment link is not active");
        }
        link.setStatus(PaymentLinkStatus.PAID);
        link.setPaidAt(OffsetDateTime.now());
        link = paymentLinkRepository.save(link);
        log.info("Payment link marked paid: linkId={}, token={}", link.getId(), token);
        return mapToPublic(link);
    }

    private PaymentLinkPublicResponse mapToPublic(PaymentLink link) {
        String merchantName = merchantRepository.findById(link.getMerchantId())
                .map(Merchant::getBusinessName)
                .orElse(null);
        return PaymentLinkPublicResponse.builder()
                .token(link.getToken())
                .merchantId(link.getMerchantId())
                .merchantName(merchantName)
                .amount(link.getAmount())
                .description(link.getDescription())
                .status(link.getStatus())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private PaymentLinkResponse mapToResponse(PaymentLink link) {
        return PaymentLinkResponse.builder()
                .id(link.getId())
                .merchantId(link.getMerchantId())
                .token(link.getToken())
                .amount(link.getAmount())
                .description(link.getDescription())
                .customerPhone(link.getCustomerPhone())
                .customerName(link.getCustomerName())
                .status(link.getStatus())
                .singleUse(link.isSingleUse())
                .paidAt(link.getPaidAt())
                .expiresAt(link.getExpiresAt())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private String generateToken() {
        String token;
        do {
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 12; i++) {
                sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
            }
            token = sb.toString();
        } while (paymentLinkRepository.existsByToken(token));
        return token;
    }
}
