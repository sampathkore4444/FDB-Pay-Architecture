package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.client.TransferServiceClient;
import com.fdbpay.merchant.service.dto.request.MerchantReviewRequest;
import com.fdbpay.merchant.service.dto.request.ReviewReplyRequest;
import com.fdbpay.merchant.service.dto.response.CustomerInsightResponse;
import com.fdbpay.merchant.service.dto.response.MerchantReviewResponse;
import com.fdbpay.merchant.service.model.LoyaltySettings;
import com.fdbpay.merchant.service.model.MerchantReview;
import com.fdbpay.merchant.service.model.enums.ReviewStatus;
import com.fdbpay.merchant.service.repository.LoyaltySettingsRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.MerchantReviewRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final MerchantReviewRepository reviewRepository;
    private final MerchantRepository merchantRepository;
    private final LoyaltySettingsRepository loyaltySettingsRepository;
    private final TransferServiceClient transferServiceClient;
    private final AuditService auditService;

    public List<CustomerInsightResponse> insights(UUID merchantId, UUID walletId) {
        requireMerchant(merchantId);
        LoyaltySettings loyalty = loyaltySettingsRepository.findByMerchantId(merchantId)
                .orElseGet(() -> LoyaltySettings.builder().merchantId(merchantId).pointsPerMmk(1).rewardThresholdPoints(1000).rewardValue(1000L).enabled(true).build());

        List<CustomerInsightResponse> result = new ArrayList<>();
        for (Map<String, Object> row : transferServiceClient.getCustomers(walletId)) {
            String wallet = String.valueOf(row.getOrDefault("walletId", ""));
            long spend = toLong(row.get("amount"));
            int count = (int) toLong(row.get("count"));
            result.add(CustomerInsightResponse.builder()
                    .walletId(wallet)
                    .totalSpend(spend)
                    .transactionCount(count)
                    .lastPurchaseAt(parseInstant(row.get("lastPurchaseAt")))
                    .tier(tierFor(spend))
                    .loyaltyPoints(spend / 1000L * loyalty.getPointsPerMmk())
                    .build());
        }
        return result;
    }

    // ---- Reviews ----

    public List<MerchantReviewResponse> listReviews(UUID merchantId) {
        requireMerchant(merchantId);
        return reviewRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapReview).toList();
    }

    @Transactional
    public MerchantReviewResponse createReview(UUID merchantId, MerchantReviewRequest request) {
        requireMerchant(merchantId);
        MerchantReview review = MerchantReview.builder()
                .merchantId(merchantId)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.PUBLISHED)
                .build();
        review = reviewRepository.save(review);
        return mapReview(review);
    }

    @Transactional
    public MerchantReviewResponse reply(UUID merchantId, UUID reviewId, ReviewReplyRequest request) {
        MerchantReview review = getOwnedReview(merchantId, reviewId);
        review.setAdminReply(request.getReply());
        review = reviewRepository.save(review);
        auditService.log(merchantId, "OWNER", null, null, "REPLY", "REVIEW", reviewId.toString(), "Replied to customer review");
        return mapReview(review);
    }

    @Transactional
    public void deleteReview(UUID merchantId, UUID reviewId) {
        MerchantReview review = getOwnedReview(merchantId, reviewId);
        reviewRepository.delete(review);
    }

    private MerchantReview getOwnedReview(UUID merchantId, UUID reviewId) {
        MerchantReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId.toString()));
        if (!review.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Review does not belong to this merchant");
        }
        return review;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private String tierFor(long spend) {
        if (spend >= 2_000_000L) return "PLATINUM";
        if (spend >= 500_000L) return "GOLD";
        if (spend >= 100_000L) return "SILVER";
        return "BRONZE";
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private java.time.OffsetDateTime parseInstant(Object value) {
        if (value == null) return null;
        try {
            return java.time.OffsetDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private MerchantReviewResponse mapReview(MerchantReview review) {
        return MerchantReviewResponse.builder()
                .id(review.getId())
                .merchantId(review.getMerchantId())
                .customerName(review.getCustomerName())
                .customerPhone(review.getCustomerPhone())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .adminReply(review.getAdminReply())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
