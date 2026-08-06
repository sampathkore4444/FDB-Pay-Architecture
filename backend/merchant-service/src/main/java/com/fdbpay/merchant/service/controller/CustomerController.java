package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.MerchantReviewRequest;
import com.fdbpay.merchant.service.dto.request.ReviewReplyRequest;
import com.fdbpay.merchant.service.dto.response.CustomerInsightResponse;
import com.fdbpay.merchant.service.dto.response.MerchantReviewResponse;
import com.fdbpay.merchant.service.dto.response.SegmentSummaryResponse;
import com.fdbpay.merchant.service.service.CustomerService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/customers")
    public ApiResponse<List<CustomerInsightResponse>> insights(
            @RequestParam UUID userId,
            @RequestParam UUID walletId) {
        return ApiResponse.success(customerService.insights(accessHelper.resolveMerchantId(userId), walletId));
    }

    @GetMapping("/customers/segments")
    public ApiResponse<List<SegmentSummaryResponse>> segments(
            @RequestParam UUID userId,
            @RequestParam UUID walletId) {
        return ApiResponse.success(customerService.segments(accessHelper.resolveMerchantId(userId), walletId));
    }

    @GetMapping("/reviews")
    public ApiResponse<List<MerchantReviewResponse>> listReviews(@RequestParam UUID userId) {
        return ApiResponse.success(customerService.listReviews(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MerchantReviewResponse> createReview(@RequestParam UUID userId, @Valid @RequestBody MerchantReviewRequest request) {
        return ApiResponse.success(customerService.createReview(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/reviews/{reviewId}/reply")
    public ApiResponse<MerchantReviewResponse> reply(@RequestParam UUID userId, @PathVariable UUID reviewId,
                                                     @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.success(customerService.reply(accessHelper.resolveMerchantId(userId), reviewId, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(@RequestParam UUID userId, @PathVariable UUID reviewId) {
        customerService.deleteReview(accessHelper.resolveMerchantId(userId), reviewId);
        return ApiResponse.success(null);
    }
}
