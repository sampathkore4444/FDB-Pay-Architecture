package com.fdbpay.merchant.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdbpay.merchant.service.dto.response.BestSellerResponse;
import com.fdbpay.merchant.service.dto.response.CustomerDetailResponse;
import com.fdbpay.merchant.service.dto.response.CustomerNoteResponse;
import com.fdbpay.merchant.service.dto.response.CustomerTimelineEntry;
import com.fdbpay.merchant.service.dto.response.RepeatCustomerResponse;
import com.fdbpay.merchant.service.model.CustomerNote;
import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.repository.CustomerNoteRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.merchant.service.repository.RefundRepository;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerInsightService {

    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final CustomerNoteRepository noteRepository;
    private final MerchantRepository merchantRepository;
    private final com.fdbpay.merchant.service.repository.ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CustomerDetailResponse getCustomerDetail(UUID merchantId, String phone) {
        requireMerchant(merchantId);
        List<Order> orders = orderRepository.findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(merchantId, phone)
                .stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
        long totalSpent = orders.stream().mapToLong(Order::getTotal).sum();
        int count = orders.size();
        long refunded = refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(r -> r.getCustomerPhone() != null && r.getCustomerPhone().equals(phone))
                .mapToLong(r -> r.getAmount()).sum();
        int refundCount = (int) refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(r -> r.getCustomerPhone() != null && r.getCustomerPhone().equals(phone))
                .count();
        int lastDays = count == 0 ? -1 : (int) Duration.between(orders.get(0).getCreatedAt(), OffsetDateTime.now()).toDays();
        boolean churnRisk = count > 0 && lastDays > 60;

        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (Order order : orders) {
            String key = order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            byMonth.merge(key, order.getTotal(), Long::sum);
        }
        return CustomerDetailResponse.builder()
                .phone(phone)
                .name(orders.isEmpty() ? null : orders.get(0).getCustomerName())
                .totalSpent(totalSpent)
                .orderCount(count)
                .refundCount(refundCount)
                .refundedAmount(refunded)
                .avgOrderValue(count == 0 ? 0 : totalSpent / count)
                .lastOrderDaysAgo(lastDays)
                .churnRisk(churnRisk)
                .byMonth(byMonth)
                .build();
    }

    public List<CustomerNoteResponse> listNotes(UUID merchantId, String phone) {
        requireMerchant(merchantId);
        return noteRepository.findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(merchantId, phone)
                .stream().map(this::mapNote).toList();
    }

    @Transactional
    public CustomerNoteResponse addNote(UUID merchantId, String phone, String note, String createdBy) {
        requireMerchant(merchantId);
        CustomerNote saved = noteRepository.save(CustomerNote.builder()
                .merchantId(merchantId)
                .customerPhone(phone)
                .note(note)
                .createdBy(createdBy)
                .build());
        return mapNote(saved);
    }

    public List<CustomerTimelineEntry> getTimeline(UUID merchantId, String phone) {
        requireMerchant(merchantId);
        List<CustomerTimelineEntry> entries = new ArrayList<>();
        for (Order order : orderRepository.findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(merchantId, phone)) {
            entries.add(CustomerTimelineEntry.builder()
                    .type("ORDER")
                    .title("Order " + order.getId().toString().substring(0, 8))
                    .detail(order.getStatus() + " - " + order.getTotal() + " MMK")
                    .at(order.getCreatedAt())
                    .build());
        }
        refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(r -> r.getCustomerPhone() != null && r.getCustomerPhone().equals(phone))
                .forEach(r -> entries.add(CustomerTimelineEntry.builder()
                        .type("REFUND")
                        .title("Refund " + r.getAmount() + " MMK")
                        .detail(r.getStatus() + (r.getReason() == null ? "" : " - " + r.getReason()))
                        .at(r.getCreatedAt())
                        .build()));
        for (CustomerNote note : noteRepository.findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(merchantId, phone)) {
            entries.add(CustomerTimelineEntry.builder()
                    .type("NOTE")
                    .title("Note")
                    .detail(note.getNote())
                    .at(note.getCreatedAt())
                    .build());
        }
        entries.sort(Comparator.comparing(CustomerTimelineEntry::getAt).reversed());
        return entries;
    }

    public List<BestSellerResponse> bestSellers(UUID merchantId, int limit) {
        requireMerchant(merchantId);
        Map<UUID, long[]> stats = new LinkedHashMap<>();
        for (Order order : orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PENDING) {
                continue;
            }
            try {
                List<Map<String, Object>> items = objectMapper.readValue(order.getItems(), List.class);
                for (Map<String, Object> item : items) {
                    UUID productId = UUID.fromString(String.valueOf(item.get("productId")));
                    long qty = ((Number) item.get("quantity")).longValue();
                    long unitPrice = ((Number) item.get("unitPrice")).longValue();
                    stats.computeIfAbsent(productId, k -> new long[]{0, 0L});
                    stats.get(productId)[0] += qty;
                    stats.get(productId)[1] += qty * unitPrice;
                }
            } catch (Exception e) {
                log.warn("Could not parse items for order {}", order.getId(), e);
            }
        }
        return stats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(limit)
                .map(e -> BestSellerResponse.builder()
                        .productId(e.getKey().toString())
                        .productName(productName(e.getKey()))
                        .unitsSold(e.getValue()[0])
                        .revenue(e.getValue()[1])
                        .build())
                .toList();
    }

    public List<RepeatCustomerResponse> repeatCustomers(UUID merchantId, int limit) {
        requireMerchant(merchantId);
        Map<String, List<Order>> byCustomer = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .filter(o -> o.getCustomerPhone() != null && !o.getCustomerPhone().isBlank()
                        && o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(Order::getCustomerPhone));
        int totalCustomers = byCustomer.size();
        return byCustomer.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .map(e -> RepeatCustomerResponse.builder()
                        .customerPhone(e.getKey())
                        .orderCount(e.getValue().size())
                        .totalSpent(e.getValue().stream().mapToLong(Order::getTotal).sum())
                        .repeatRate(totalCustomers == 0 ? 0 : Math.round(e.getValue().size() * 100.0 / totalCustomers) / 100.0)
                        .build())
                .toList();
    }

    private String productName(UUID productId) {
        return productRepository.findById(productId).map(p -> p.getName()).orElse("Deleted product");
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private CustomerNoteResponse mapNote(CustomerNote note) {
        return CustomerNoteResponse.builder()
                .id(note.getId())
                .customerPhone(note.getCustomerPhone())
                .note(note.getNote())
                .createdBy(note.getCreatedBy())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
