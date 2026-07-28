package com.fdbpay.transfer.service.dto.response;

import com.fdbpay.transfer.service.model.enums.MoneyRequestStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyRequestResponse {

    private UUID id;
    private UUID requesterUserId;
    private String requesterName;
    private String targetPhone;
    private Long amount;
    private String description;
    private MoneyRequestStatus status;
    private String paymentLink;
    private UUID paymentId;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
