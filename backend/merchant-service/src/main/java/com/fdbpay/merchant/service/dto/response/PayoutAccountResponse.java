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
public class PayoutAccountResponse {

    private UUID id;
    private UUID merchantId;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String branch;
    private Boolean isDefault;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
}
