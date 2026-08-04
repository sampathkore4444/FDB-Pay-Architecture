package com.fdbpay.bill.service.dto.refdata;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDataLookup {

    private String code;
    private String description;
    private List<RefDataValue> values;
}
