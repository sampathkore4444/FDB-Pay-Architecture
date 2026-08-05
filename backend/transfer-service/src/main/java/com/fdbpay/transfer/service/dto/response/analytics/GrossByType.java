package com.fdbpay.transfer.service.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrossByType {
    private String type;
    private int count;
    private Long volume;
    private Long fees;
}
