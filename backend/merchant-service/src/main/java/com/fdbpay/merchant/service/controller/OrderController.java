package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.CreateOrderRequest;
import com.fdbpay.merchant.service.dto.response.OrderResponse;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.service.OrderService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<OrderResponse>> list(@RequestParam UUID userId,
                                                 @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.success(orderService.listOrders(accessHelper.resolveMerchantId(userId), status));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@RequestParam UUID userId, @PathVariable UUID orderId) {
        return ApiResponse.success(orderService.getOrder(accessHelper.resolveMerchantId(userId), orderId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> create(@RequestParam UUID userId, @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/{orderId}/pay")
    public ApiResponse<OrderResponse> markPaid(@RequestParam UUID userId, @PathVariable UUID orderId) {
        return ApiResponse.success(orderService.markPaid(accessHelper.resolveMerchantId(userId), orderId));
    }

    @PutMapping("/{orderId}/fulfill")
    public ApiResponse<OrderResponse> fulfill(@RequestParam UUID userId, @PathVariable UUID orderId) {
        return ApiResponse.success(orderService.fulfill(accessHelper.resolveMerchantId(userId), orderId));
    }

    @PutMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(@RequestParam UUID userId, @PathVariable UUID orderId) {
        return ApiResponse.success(orderService.cancel(accessHelper.resolveMerchantId(userId), orderId));
    }
}
