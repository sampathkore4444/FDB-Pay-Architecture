package com.fdbpay.referencedata.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceValueResponse {

    private UUID id;
    private String value;
    private String code;
    private Integer sortOrder;
    private Boolean active;
}
