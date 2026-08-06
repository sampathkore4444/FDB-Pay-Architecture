package com.fdbpay.merchant.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotBlank(message = "Frequency is required")
    private String frequency;

    private String format;

    private String email;

    private Boolean enabled;
}
