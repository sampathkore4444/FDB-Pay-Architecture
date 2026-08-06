package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbTestRequest {

    @NotBlank(message = "Test name is required")
    private String name;

    @NotNull(message = "Minimum spend is required")
    private Long minSpend;

    @NotNull(message = "Max uses is required")
    private Integer maxUses;

    private OffsetDateTime validTo;

    @Valid
    private List<AbVariantRequest> variants;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AbVariantRequest {

        @NotBlank(message = "Variant code is required")
        private String code;

        @NotBlank(message = "Variant type is required")
        private String type;

        @NotNull(message = "Variant value is required")
        private Long value;
    }
}
