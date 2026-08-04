package com.fdbpay.bill.service.dto.refdata;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDataValue {

    private UUID id;
    private String value;
    private String code;
}
