package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RollingReserveInfo {
    private Integer percent;
    private Long heldThisPeriod;
    private Long releasedThisPeriod;
    private Long currentBalance;
}
