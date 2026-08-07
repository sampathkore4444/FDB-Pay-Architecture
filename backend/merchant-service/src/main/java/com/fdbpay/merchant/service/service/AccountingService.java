package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.response.AccountingExportResponse;
import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.model.enums.RefundStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.merchant.service.repository.RefundRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingService {

    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    public List<AccountingExportResponse> export(UUID merchantId, OffsetDateTime from, OffsetDateTime to) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        OffsetDateTime start = from == null ? OffsetDateTime.now().minusMonths(1) : from;
        OffsetDateTime end = to == null ? OffsetDateTime.now() : to;

        List<Order> orders = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                .toList();
        long grossSales = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .mapToLong(Order::getTotal).sum();
        long taxCollected = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .mapToLong(Order::getTax).sum();
        long refunds = refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(start) && !r.getCreatedAt().isAfter(end))
                .mapToLong(r -> r.getAmount()).sum();

        List<AccountingExportResponse> lines = new ArrayList<>();
        lines.add(AccountingExportResponse.builder().accountCode("4000").description("Sales revenue (gross)").amount(grossSales).build());
        lines.add(AccountingExportResponse.builder().accountCode("2100").description("Output VAT / sales tax collected").amount(taxCollected).build());
        lines.add(AccountingExportResponse.builder().accountCode("5000").description("Refunds issued").amount(-refunds).build());
        long net = grossSales - taxCollected - refunds;
        lines.add(AccountingExportResponse.builder().accountCode("5000-NET").description("Net sales (excl. tax, after refunds)").amount(net).build());
        return lines;
    }
}
