package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private UUID id;
    private UUID merchantId;
    private String name;
    private Long price;
    private String description;
    private String category;
    private String imageUrl;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
}
