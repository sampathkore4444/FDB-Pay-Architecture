package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorePerformance {

    private String storeId;
    private Long amount;
    private Integer count;
}
