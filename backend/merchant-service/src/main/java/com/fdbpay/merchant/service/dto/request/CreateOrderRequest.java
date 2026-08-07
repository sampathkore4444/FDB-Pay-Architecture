package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<OrderItemRequest> items;

    private String customerPhone;

    private String customerName;

    @PositiveOrZero(message = "Tax must be non-negative")
    private Long tax;

    @PositiveOrZero(message = "Tax rate must be non-negative")
    private Integer taxRate;
}
