package com.fdbpay.merchant.service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class TransferAnalyticsClient {

    private static final String BASE = "http://transfer-service/transfer/analytics";

    private final WebClient webClient;

    public TransferAnalyticsClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE).build();
    }

    public Map<String, Object> getSummary(UUID walletId, LocalDate from, LocalDate to) {
        try {
            return webClient.get()
                    .uri(uri -> uri.path("/summary")
                            .queryParam("walletId", walletId)
                            .queryParam("startDate", from)
                            .queryParam("endDate", to)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch analytics summary for wallet {}: {}", walletId, e.getMessage());
            return Map.of();
        }
    }
}
