package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateResponse {

    private UUID id;
    private UUID merchantId;
    private String name;
    private String reportType;
    private String frequency;
    private String format;
    private String email;
    private Boolean enabled;
    private OffsetDateTime createdAt;
}
