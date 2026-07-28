package com.fdbpay.merchant.service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantDirectoryEntry {

    private UUID id;
    private String businessName;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double rating;
    private String qrStaticUrl;
}
