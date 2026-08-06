package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentSummaryResponse {

    private String segment;
    private Long customerCount;
    private Long totalSpend;
}
