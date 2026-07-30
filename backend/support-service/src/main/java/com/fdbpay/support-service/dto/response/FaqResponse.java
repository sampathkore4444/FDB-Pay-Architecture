package com.fdbpay.support.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqResponse {
    private UUID id;
    private String question;
    private String answer;
    private String category;
    private int sortOrder;
}
