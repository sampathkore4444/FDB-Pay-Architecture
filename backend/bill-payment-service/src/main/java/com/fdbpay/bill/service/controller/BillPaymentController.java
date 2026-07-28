package com.fdbpay.bill.service.controller;

import com.fdbpay.bill.service.dto.request.BillPaymentRequest;
import com.fdbpay.bill.service.dto.response.BillerResponse;
import com.fdbpay.bill.service.dto.response.BillLookupResponse;
import com.fdbpay.bill.service.dto.response.BillPaymentResponse;
import com.fdbpay.bill.service.service.BillPaymentService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    @GetMapping("/categories")
    public ApiResponse<List<BillerResponse>> getCategories() {
        return ApiResponse.success(billPaymentService.getCategories());
    }

    @GetMapping("/billers")
    public ApiResponse<List<BillerResponse>> getBillers(
            @RequestParam(required = false) String category) {
        return ApiResponse.success(billPaymentService.getBillers(category));
    }

    @PostMapping("/lookup")
    public ApiResponse<BillLookupResponse> lookupBill(
            @RequestParam UUID billerId,
            @RequestParam String accountNumber) {
        return ApiResponse.success(billPaymentService.lookupBill(billerId, accountNumber));
    }

    @PostMapping("/pay")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BillPaymentResponse> payBill(
            @RequestParam UUID userId,
            @Valid @RequestBody BillPaymentRequest request) {
        return ApiResponse.success(billPaymentService.payBill(userId, request));
    }

    @GetMapping("/history")
    public ApiResponse<Page<BillPaymentResponse>> getHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(billPaymentService.getHistory(userId, page, size));
    }
}
