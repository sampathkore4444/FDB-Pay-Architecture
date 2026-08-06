package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.model.enums.DiscountType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCodeResponse {

    private UUID id;
    private UUID merchantId;
    private String code;
    private DiscountType type;
    private Long value;
    private Long minSpend;
    private Integer maxUses;
    private Integer usedCount;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
}
