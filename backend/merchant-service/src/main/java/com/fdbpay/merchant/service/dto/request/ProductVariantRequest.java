package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    private String name;

    private Long priceDelta;

    private Long quantity;
}
