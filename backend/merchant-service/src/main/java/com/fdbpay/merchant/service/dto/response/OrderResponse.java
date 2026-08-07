package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.OrderStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private UUID merchantId;
    private UUID storeId;
    private String customerPhone;
    private String customerName;
    private Object items;
    private Long subtotal;
    private Long tax;
    private Integer taxRate;
    private Long total;
    private OrderStatus status;
    private Long refundAmount;
    private OffsetDateTime paidAt;
    private OffsetDateTime fulfilledAt;
    private OffsetDateTime createdAt;
}
