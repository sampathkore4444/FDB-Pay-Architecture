package com.fdbpay.merchant.service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BestSellerResponse {

    private String productId;
    private String productName;
    private long unitsSold;
    private Long revenue;
}
