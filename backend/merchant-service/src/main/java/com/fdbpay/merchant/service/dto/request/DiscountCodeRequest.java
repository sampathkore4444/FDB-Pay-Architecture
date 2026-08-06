package com.fdbpay.merchant.service.dto.request;

import com.fdbpay.merchant.service.model.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCodeRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Type is required")
    private DiscountType type;

    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    private Long value;

    private Long minSpend;

    private Integer maxUses;

    private OffsetDateTime validFrom;

    private OffsetDateTime validTo;
}
