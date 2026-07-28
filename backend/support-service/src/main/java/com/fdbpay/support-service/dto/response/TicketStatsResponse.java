package com.fdbpay.support.service.dto.response;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatsResponse {

    private Long totalOpen;
    private Long totalResolved;
    private Double avgResponseTimeHours;
    private Map<String, Long> byCategory;
    private Map<String, Long> byStatus;
}
