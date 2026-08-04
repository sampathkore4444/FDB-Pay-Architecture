package com.fdbpay.bill.service.dto.refdata;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDataType {

    private UUID id;
    private String code;
    private String description;
    private Boolean active;
    private Long valueCount;
}
