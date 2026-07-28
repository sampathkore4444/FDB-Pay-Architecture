package com.fdbpay.bill.service.dto.response;

import com.fdbpay.bill.service.model.AirtimeTopup;
import com.fdbpay.bill.service.model.enums.AirtimeProvider;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirtimeTopupResponse {

    private UUID id;
    private String phone;
    private Long amount;
    private AirtimeProvider provider;
    private AirtimeTopup.TopupStatus status;
    private String transactionRef;
    private OffsetDateTime createdAt;
}
