package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private String name;
    private Long priceDelta;
    private Long quantity;
    private OffsetDateTime createdAt;
}
