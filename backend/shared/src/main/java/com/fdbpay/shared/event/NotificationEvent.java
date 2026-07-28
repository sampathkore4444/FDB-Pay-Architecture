package com.fdbpay.shared.event;

import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {
    private UUID userId;
    private String channel;
    private String type;
    private String title;
    private String body;
    private String phone;
    private String email;
    private OffsetDateTime timestamp;
}
