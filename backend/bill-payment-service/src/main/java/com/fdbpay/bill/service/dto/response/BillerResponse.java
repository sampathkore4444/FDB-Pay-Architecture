package com.fdbpay.bill.service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillerResponse {

    private UUID id;
    private String name;
    private String category;
    private String logo;
    private String description;
}
