package com.fdbpay.support.service.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignManagerRequest {

    private UUID managerId;
}
