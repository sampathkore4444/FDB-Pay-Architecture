package com.fdbpay.dispute.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeStatsResponse {

    private long totalOpen;
    private long totalResolved;
    private double avgResolutionHours;
}
