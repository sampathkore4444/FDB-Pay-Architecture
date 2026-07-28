package com.fdbpay.reporting.service.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceReport {

    private String month;
    private Long amlAlerts;
    private Long strFiled;
}
