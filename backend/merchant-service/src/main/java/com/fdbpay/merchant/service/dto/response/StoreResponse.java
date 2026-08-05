package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.StoreStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {

    private UUID id;
    private UUID merchantId;
    private String name;
    private String address;
    private String city;
    private String phone;
    private StoreStatus status;
    private OffsetDateTime createdAt;
}
