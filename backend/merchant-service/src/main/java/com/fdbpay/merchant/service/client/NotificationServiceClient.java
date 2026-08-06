package com.fdbpay.merchant.service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class NotificationServiceClient {

    private static final String BASE = "http://notification-service";

    private final WebClient webClient;

    public NotificationServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE).build();
    }

    public void sendPush(UUID userId, String title, String body) {
        try {
            webClient.post()
                    .uri("/notifications/send-push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("userId", userId.toString(), "title", title, "body", body))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Push notification failed for userId={}: {}", userId, e.getMessage());
        }
    }

    public void sendBulk(List<UUID> userIds, String title, String body) {
        try {
            webClient.post()
                    .uri("/notifications/send-bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "userIds", userIds.stream().map(UUID::toString).toList(),
                            "title", title,
                            "body", body))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Bulk notification failed for {} users: {}", userIds.size(), e.getMessage());
        }
    }
}
