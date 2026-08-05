package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.merchant.service.client.TransferAnalyticsClient;
import com.fdbpay.merchant.service.dto.request.FinancingApplicationRequest;
import com.fdbpay.merchant.service.dto.response.FinancingApplicationResponse;
import com.fdbpay.merchant.service.dto.response.FinancingEligibilityResponse;
import com.fdbpay.merchant.service.model.FinancingApplication;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.enums.FinancingStatus;
import com.fdbpay.merchant.service.repository.FinancingApplicationRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.FinancingService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancingServiceImpl implements FinancingService {

    private static final int LOOKBACK_DAYS = 90;
    private static final int MAX_TERM_MONTHS = 12;

    private final FinancingApplicationRepository applicationRepository;
    private final MerchantRepository merchantRepository;
    private final TransferAnalyticsClient analyticsClient;

    @Override
    public FinancingEligibilityResponse getEligibility(UUID merchantId, UUID walletId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(LOOKBACK_DAYS);

        long totalSales = 0;
        try {
            Map<String, Object> summary = analyticsClient.getSummary(walletId, from, to);
            Object data = summary.get("data");
            if (data instanceof Map<?, ?> inner && inner.get("totalSales") != null) {
                totalSales = ((Number) inner.get("totalSales")).longValue();
            }
        } catch (Exception e) {
            log.warn("Eligibility analytics unavailable for merchant {}: {}", merchantId, e.getMessage());
        }

        long monthlyRevenue = totalSales / 3;
        long avgDailySales = totalSales / LOOKBACK_DAYS;
        long estimatedLimit = roundToTenThousand(monthlyRevenue);

        return FinancingEligibilityResponse.builder()
                .eligible(totalSales > 0)
                .monthlyRevenue(monthlyRevenue)
                .threeMonthVolume(totalSales)
                .avgDailySales(avgDailySales)
                .estimatedLimit(estimatedLimit)
                .maxTermMonths(MAX_TERM_MONTHS)
                .message(totalSales > 0
                        ? "Eligible for working capital financing"
                        : "Insufficient transaction volume in the last 90 days")
                .build();
    }

    @Override
    @Transactional
    public FinancingApplicationResponse apply(UUID merchantId, UUID walletId, FinancingApplicationRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        FinancingEligibilityResponse eligibility = getEligibility(merchantId, walletId);
        if (!eligibility.isEligible()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, eligibility.getMessage());
        }
        if (request.getRequestedAmount() > eligibility.getEstimatedLimit()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Requested amount exceeds the estimated limit of " + eligibility.getEstimatedLimit());
        }

        FinancingApplication application = FinancingApplication.builder()
                .merchantId(merchant.getId())
                .requestedAmount(request.getRequestedAmount())
                .termMonths(request.getTermMonths())
                .purpose(request.getPurpose())
                .monthlyRevenue(eligibility.getMonthlyRevenue())
                .estimatedLimit(eligibility.getEstimatedLimit())
                .status(FinancingStatus.PENDING)
                .build();
        application = applicationRepository.save(application);
        log.info("Financing application created: id={}, merchantId={}, amount={}",
                application.getId(), merchantId, request.getRequestedAmount());
        return mapToResponse(application);
    }

    @Override
    public List<FinancingApplicationResponse> getApplications(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        return applicationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public FinancingApplicationResponse getApplication(UUID merchantId, UUID applicationId) {
        FinancingApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("FinancingApplication", applicationId.toString()));
        if (!application.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Application does not belong to this merchant");
        }
        return mapToResponse(application);
    }

    private long roundToTenThousand(long value) {
        return Math.max(0, value / 10000) * 10000;
    }

    private FinancingApplicationResponse mapToResponse(FinancingApplication application) {
        return FinancingApplicationResponse.builder()
                .id(application.getId())
                .merchantId(application.getMerchantId())
                .requestedAmount(application.getRequestedAmount())
                .termMonths(application.getTermMonths())
                .purpose(application.getPurpose())
                .monthlyRevenue(application.getMonthlyRevenue())
                .estimatedLimit(application.getEstimatedLimit())
                .status(application.getStatus())
                .adminNote(application.getAdminNote())
                .createdAt(application.getCreatedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }
}
