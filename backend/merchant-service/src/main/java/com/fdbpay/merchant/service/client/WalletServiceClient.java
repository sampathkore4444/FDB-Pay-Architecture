package com.fdbpay.merchant.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WalletServiceClient {

    private static final String BASE = "http://wallet-service";

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WalletServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE).build();
    }

    public Long getAvailableBalance(UUID userId) {
        try {
            JsonNode node = webClient.get()
                    .uri(uri -> uri.path("/wallet").queryParam("userId", userId).build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (node != null && node.path("data").has("balanceAvailable")) {
                return node.path("data").path("balanceAvailable").asLong();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch wallet balance for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    public UUID getWalletId(UUID userId) {
        try {
            JsonNode node = webClient.get()
                    .uri(uri -> uri.path("/wallet").queryParam("userId", userId).build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (node != null && node.path("data").has("id")) {
                return UUID.fromString(node.path("data").path("id").asText());
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch wallet for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    public boolean debit(UUID walletId, Long amount, String description, UUID txnId) {
        try {
            Map<String, Object> body = Map.of(
                    "walletId", walletId.toString(),
                    "amount", amount,
                    "description", description,
                    "txnId", txnId.toString());
            webClient.post()
                    .uri("/wallet/debit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("Wallet debit failed for walletId={}: {}", walletId, e.getMessage());
            return false;
        }
    }
}
