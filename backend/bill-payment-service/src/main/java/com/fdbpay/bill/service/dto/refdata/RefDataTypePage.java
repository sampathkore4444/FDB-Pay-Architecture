package com.fdbpay.bill.service.dto.refdata;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDataTypePage {

    private List<RefDataType> content;
}
