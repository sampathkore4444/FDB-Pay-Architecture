package com.fdbpay.merchant.service.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSearchRequest {

    private String query;

    private String category;

    private Double latitude;

    private Double longitude;

    private Double radius;
}
