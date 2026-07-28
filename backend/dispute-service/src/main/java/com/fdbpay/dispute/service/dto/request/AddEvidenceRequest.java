package com.fdbpay.dispute.service.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddEvidenceRequest {

    private String fileUrl;
    private String description;
}
