package com.fdbpay.merchant.service.dto.response;

import com.fdbpay.merchant.service.model.ReferralRegistration;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralPerformanceResponse {

    private long totalRegistrations;
    private long convertedRegistrations;
    private long pendingRegistrations;
    private double conversionRatePct;
    private Long totalBonusPaid;
    private List<ReferralRegistration> registrations;
}
