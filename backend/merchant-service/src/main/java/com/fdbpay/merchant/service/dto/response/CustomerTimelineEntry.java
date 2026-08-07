package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTimelineEntry {

    private String type;
    private String title;
    private String detail;
    private OffsetDateTime at;
}
