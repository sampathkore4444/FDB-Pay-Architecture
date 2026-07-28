package com.fdbpay.audit.service.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSummaryResponse {

    private long totalActions;
    private Map<String, Long> byAction;
    private Map<String, Long> byActorType;
    private LocalDate startDate;
    private LocalDate endDate;
}
