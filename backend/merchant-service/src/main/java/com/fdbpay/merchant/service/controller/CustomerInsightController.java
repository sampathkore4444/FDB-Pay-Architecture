package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.CustomerNoteRequest;
import com.fdbpay.merchant.service.dto.response.CustomerDetailResponse;
import com.fdbpay.merchant.service.dto.response.CustomerNoteResponse;
import com.fdbpay.merchant.service.dto.response.CustomerTimelineEntry;
import com.fdbpay.merchant.service.service.CustomerInsightService;
import com.fdbpay.merchant.service.service.OrderService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/customers")
@RequiredArgsConstructor
public class CustomerInsightController {

    private final CustomerInsightService customerInsightService;
    private final OrderService orderService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping("/{phone}")
    public ApiResponse<CustomerDetailResponse> detail(@RequestParam UUID userId, @PathVariable String phone) {
        return ApiResponse.success(customerInsightService.getCustomerDetail(accessHelper.resolveMerchantId(userId), phone));
    }

    @GetMapping("/{phone}/orders")
    public ApiResponse<?> orders(@RequestParam UUID userId, @PathVariable String phone) {
        return ApiResponse.success(orderService.listCustomerOrders(accessHelper.resolveMerchantId(userId), phone));
    }

    @GetMapping("/{phone}/timeline")
    public ApiResponse<List<CustomerTimelineEntry>> timeline(@RequestParam UUID userId, @PathVariable String phone) {
        return ApiResponse.success(customerInsightService.getTimeline(accessHelper.resolveMerchantId(userId), phone));
    }

    @GetMapping("/{phone}/notes")
    public ApiResponse<List<CustomerNoteResponse>> notes(@RequestParam UUID userId, @PathVariable String phone) {
        return ApiResponse.success(customerInsightService.listNotes(accessHelper.resolveMerchantId(userId), phone));
    }

    @PostMapping("/{phone}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerNoteResponse> addNote(@RequestParam UUID userId, @PathVariable String phone,
                                                     @RequestParam(required = false, defaultValue = "owner") String createdBy,
                                                     @Valid @RequestBody CustomerNoteRequest request) {
        return ApiResponse.success(customerInsightService.addNote(accessHelper.resolveMerchantId(userId), phone, request.getNote(), createdBy));
    }
}
