package com.fdbpay.audit.service.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSummaryResponse {

    private long totalActions;
    private long totalEvents;
    private long uniqueActors;
    private List<TopAction> topActions;
    private Map<String, Long> byAction;
    private Map<String, Long> byActorType;
    private LocalDate startDate;
    private LocalDate endDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopAction {
        private String action;
        private long count;
    }
}
