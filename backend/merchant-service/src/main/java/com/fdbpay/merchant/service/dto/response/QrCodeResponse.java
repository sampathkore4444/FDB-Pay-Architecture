package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCodeResponse {

    private UUID merchantId;
    private String qrUrl;
    private String deepLink;
}
