package com.fdbpay.referencedata.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceDataLookupResponse {

    private String code;
    private String description;
    private List<LookupValue> values;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LookupValue {
        private UUID id;
        private String value;
        private String code;
    }
}
