package com.fdbpay.corporate.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollScheduleResponse {

    private UUID id;
    private LocalDate scheduledDate;
    private String status;
    private OffsetDateTime createdAt;
}
