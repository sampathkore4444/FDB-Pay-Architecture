package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.enums.RecurringInterval;
import com.fdbpay.merchant.service.model.enums.RecurringPlanStatus;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringPlanResponse {

    private UUID id;
    private UUID merchantId;
    private String name;
    private String description;
    private Long amount;
    private String customerPhone;
    private String customerName;
    private RecurringInterval interval;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private LocalTime time;
    private RecurringPlanStatus status;
    private Integer maxCharges;
    private Integer chargeCount;
    private OffsetDateTime nextRunAt;
    private OffsetDateTime lastChargeAt;
    private OffsetDateTime createdAt;
}
