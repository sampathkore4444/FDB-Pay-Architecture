package com.fdbpay.merchant.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdbpay.merchant.service.client.WalletServiceClient;
import com.fdbpay.merchant.service.dto.request.CreateOrderRequest;
import com.fdbpay.merchant.service.dto.request.OrderItemRequest;
import com.fdbpay.merchant.service.dto.response.OrderResponse;
import com.fdbpay.merchant.service.model.DigitalDelivery;
import com.fdbpay.merchant.service.model.Order;
import com.fdbpay.merchant.service.model.Product;
import com.fdbpay.merchant.service.model.ProductVariant;
import com.fdbpay.merchant.service.model.enums.OrderStatus;
import com.fdbpay.merchant.service.repository.DigitalDeliveryRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.OrderRepository;
import com.fdbpay.merchant.service.repository.ProductRepository;
import com.fdbpay.merchant.service.repository.ProductVariantRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final DigitalDeliveryRepository deliveryRepository;
    private final MerchantRepository merchantRepository;
    private final WalletServiceClient walletServiceClient;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public List<OrderResponse> listOrders(UUID merchantId, OrderStatus status) {
        requireMerchant(merchantId);
        List<Order> orders = status == null
                ? orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                : orderRepository.findByMerchantIdAndStatusOrderByCreatedAtDesc(merchantId, status);
        return orders.stream().map(this::mapToResponse).toList();
    }

    public List<OrderResponse> listCustomerOrders(UUID merchantId, String phone) {
        requireMerchant(merchantId);
        return orderRepository.findByMerchantIdAndCustomerPhoneOrderByCreatedAtDesc(merchantId, phone)
                .stream().map(this::mapToResponse).toList();
    }

    public OrderResponse getOrder(UUID merchantId, UUID orderId) {
        Order order = getOwned(merchantId, orderId);
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(UUID merchantId, CreateOrderRequest request) {
        requireMerchant(merchantId);
        long subtotal = 0L;
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProductId().toString()));
            if (!product.getMerchantId().equals(merchantId)) {
                throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Product does not belong to this merchant");
            }
            long unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : product.getPrice();
            ProductVariant variant = null;
            if (item.getVariantId() != null) {
                variant = variantRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", item.getVariantId().toString()));
                if (!variant.getProductId().equals(product.getId())) {
                    throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Variant does not belong to product");
                }
                unitPrice += variant.getPriceDelta() == null ? 0L : variant.getPriceDelta();
            }
            long qty = item.getQuantity();
            decrementStock(product, variant, qty);
            long lineTotal = unitPrice * qty;
            subtotal += lineTotal;
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("productId", product.getId());
            line.put("name", product.getName());
            line.put("quantity", qty);
            line.put("unitPrice", unitPrice);
            line.put("variantId", item.getVariantId());
            line.put("lineTotal", lineTotal);
            items.add(line);
        }

        int taxRate = request.getTaxRate() == null ? 0 : request.getTaxRate();
        long tax = request.getTax() != null ? request.getTax() : Math.round(subtotal * taxRate / 100.0);
        long total = subtotal + tax;

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            itemsJson = items.toString();
        }

        Order order = Order.builder()
                .merchantId(merchantId)
                .customerPhone(request.getCustomerPhone())
                .customerName(request.getCustomerName())
                .items(itemsJson)
                .subtotal(subtotal)
                .tax(tax)
                .taxRate(taxRate)
                .total(total)
                .status(OrderStatus.PENDING)
                .build();
        order = orderRepository.save(order);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "ORDER", order.getId().toString(),
                "Created order for " + (request.getCustomerPhone() == null ? "walk-in customer" : request.getCustomerPhone())
                        + " totaling " + total + " MMK");

        createDeliveries(merchantId, order.getId(), request.getItems());
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse markPaid(UUID merchantId, UUID orderId) {
        Order order = getOwned(merchantId, orderId);
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.FULFILLED) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Order already paid");
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(OffsetDateTime.now());
        order = orderRepository.save(order);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "ORDER", orderId.toString(),
                "Marked order as paid");
        deliverPending(order);
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse fulfill(UUID merchantId, UUID orderId) {
        Order order = getOwned(merchantId, orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Order must be paid before fulfillment");
        }
        order.setStatus(OrderStatus.FULFILLED);
        order.setFulfilledAt(OffsetDateTime.now());
        order = orderRepository.save(order);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "ORDER", orderId.toString(),
                "Fulfilled order");
        deliverPending(order);
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse cancel(UUID merchantId, UUID orderId) {
        Order order = getOwned(merchantId, orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Only pending orders can be cancelled");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "ORDER", orderId.toString(),
                "Cancelled order");
        return mapToResponse(order);
    }

    private void decrementStock(Product product, ProductVariant variant, long qty) {
        if (variant != null) {
            if (variant.getQuantity() < qty) {
                throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Insufficient stock for variant " + variant.getSku());
            }
            variant.setQuantity(variant.getQuantity() - qty);
            variantRepository.save(variant);
        } else {
            if (product.getQuantity() < qty) {
                throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Insufficient stock for product " + product.getName());
            }
            product.setQuantity(product.getQuantity() - qty);
            productRepository.save(product);
        }
    }

    private void restoreStock(Order order) {
        try {
            List<Map<String, Object>> items = objectMapper.readValue(order.getItems(), List.class);
            for (Map<String, Object> item : items) {
                UUID productId = UUID.fromString(String.valueOf(item.get("productId")));
                long qty = ((Number) item.get("quantity")).longValue();
                productRepository.findById(productId).ifPresent(p -> {
                    p.setQuantity(p.getQuantity() + qty);
                    productRepository.save(p);
                });
            }
        } catch (Exception e) {
            log.warn("Could not restore stock for order {}", order.getId(), e);
        }
    }

    private void createDeliveries(UUID merchantId, UUID orderId, List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null && product.isDeliverable() && product.getDeliveryContent() != null) {
                deliveryRepository.save(DigitalDelivery.builder()
                        .merchantId(merchantId)
                        .orderId(orderId)
                        .productId(product.getId())
                        .content(product.getDeliveryContent())
                        .deliveredTo(null)
                        .build());
            }
        }
    }

    private void deliverPending(Order order) {
        List<DigitalDelivery> pending = deliveryRepository.findByOrderId(order.getId()).stream()
                .filter(d -> d.getStatus() == com.fdbpay.merchant.service.model.enums.DigitalDeliveryStatus.PENDING)
                .toList();
        for (DigitalDelivery delivery : pending) {
            delivery.setStatus(com.fdbpay.merchant.service.model.enums.DigitalDeliveryStatus.DELIVERED);
            delivery.setDeliveredTo(order.getCustomerPhone());
            deliveryRepository.save(delivery);
        }
        if (!pending.isEmpty()) {
            auditService.log(order.getMerchantId(), "OWNER", null, null, "DELIVER", "ORDER", order.getId().toString(),
                    "Delivered " + pending.size() + " digital item(s)");
        }
    }

    private Order getOwned(UUID merchantId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Order does not belong to this merchant");
        }
        return order;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private OrderResponse mapToResponse(Order order) {
        Object itemsObj = order.getItems();
        try {
            itemsObj = objectMapper.readValue(order.getItems(), List.class);
        } catch (Exception ignored) {
        }
        return OrderResponse.builder()
                .id(order.getId())
                .merchantId(order.getMerchantId())
                .storeId(order.getStoreId())
                .customerPhone(order.getCustomerPhone())
                .customerName(order.getCustomerName())
                .items(itemsObj)
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .taxRate(order.getTaxRate())
                .total(order.getTotal())
                .status(order.getStatus())
                .refundAmount(order.getRefundAmount())
                .paidAt(order.getPaidAt())
                .fulfilledAt(order.getFulfilledAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
