package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.StaffAccountStatus;
import com.fdbpay.merchant.service.model.enums.StaffRole;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAccountResponse {

    private UUID id;
    private UUID merchantId;
    private UUID userId;
    private StaffRole role;
    private StaffAccountStatus status;
    private Long dailyLimit;
    private OffsetDateTime createdAt;
}
