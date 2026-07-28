package com.fdbpay.controllers;

import com.fdbpay.schemas.request.BillPaymentRequest;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.schemas.response.TransactionResponse;
import com.fdbpay.services.BillPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(billPaymentService.getCategories()));
    }

    @GetMapping("/billers")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBillers(@RequestParam String category) {
        return ResponseEntity.ok(ApiResponse.success(billPaymentService.getBillers(category)));
    }

    @GetMapping("/billers/{billerId}/lookup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lookupBill(
            @PathVariable String billerId,
            @RequestParam String account) {
        return ResponseEntity.ok(ApiResponse.success(billPaymentService.lookupBill(billerId, account)));
    }

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<TransactionResponse>> payBill(
            @RequestParam UUID userId,
            @Valid @RequestBody BillPaymentRequest request) {
        TransactionResponse response = billPaymentService.payBill(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(billPaymentService.getBillPaymentHistory(userId, page, size)));
    }
}
