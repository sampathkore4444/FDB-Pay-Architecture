package com.fdbpay.promotions.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.promotions.service.dto.request.ApplyPromotionRequest;
import com.fdbpay.promotions.service.dto.request.CreatePromotionRequest;
import com.fdbpay.promotions.service.dto.request.RedeemCashbackRequest;
import com.fdbpay.promotions.service.dto.response.*;
import com.fdbpay.promotions.service.model.CashbackTransaction;
import com.fdbpay.promotions.service.model.CashbackWallet;
import com.fdbpay.promotions.service.model.Promotion;
import com.fdbpay.promotions.service.model.PromotionUsage;
import com.fdbpay.promotions.service.model.enums.CashbackTxnType;
import com.fdbpay.promotions.service.model.enums.PromotionStatus;
import com.fdbpay.promotions.service.model.enums.PromotionType;
import com.fdbpay.promotions.service.repository.*;
import com.fdbpay.promotions.service.service.PromotionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionsServiceImpl implements PromotionsService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final CashbackWalletRepository cashbackWalletRepository;
    private final CashbackTransactionRepository cashbackTransactionRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";

    @Override
    @Transactional
    public PromotionResponse createPromotion(CreatePromotionRequest request) {
        Promotion promotion = Promotion.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .fundingType(request.getFundingType())
                .merchantId(request.getMerchantId())
                .discountValue(request.getDiscountValue())
                .maxDiscount(request.getMaxDiscount())
                .minTransactionAmount(request.getMinTransactionAmount())
                .maxUsageTotal(request.getMaxUsageTotal() != null ? request.getMaxUsageTotal() : 0)
                .maxUsagePerUser(request.getMaxUsagePerUser() != null ? request.getMaxUsagePerUser() : 0)
                .usageCount(0)
                .startDate(request.getStartDate() != null ? request.getStartDate() : OffsetDateTime.now())
                .endDate(request.getEndDate() != null ? request.getEndDate() : OffsetDateTime.now().plusDays(30))
                .status(PromotionStatus.ACTIVE)
                .promoCode(request.getPromoCode())
                .build();

        promotion = promotionRepository.save(promotion);
        log.info("Promotion created: id={}, title={}, type={}", promotion.getId(), promotion.getTitle(), promotion.getType());
        return mapToResponse(promotion);
    }

    @Override
    public List<PromotionResponse> getActivePromotions(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return promotionRepository.findByStatusAndStartDateBeforeAndEndDateAfter(PromotionStatus.ACTIVE, now, now)
                .stream()
                .filter(p -> p.getMerchantId() == null || p.getMerchantId().equals(userId))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PromotionValidationResponse validatePromoCode(String promoCode, Long amount, UUID userId) {
        Promotion promotion = promotionRepository.findByPromoCode(promoCode)
                .orElse(null);

        if (promotion == null) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .discount(0L)
                    .message("Invalid promo code")
                    .build();
        }

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .discount(0L)
                    .message("Promotion is not active")
                    .build();
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .discount(0L)
                    .message("Promotion has expired or not yet started")
                    .build();
        }

        if (promotion.getMaxUsageTotal() > 0 && promotion.getUsageCount() >= promotion.getMaxUsageTotal()) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .discount(0L)
                    .message("Promotion usage limit reached")
                    .build();
        }

        if (promotion.getMaxUsagePerUser() > 0) {
            long userUsageCount = promotionUsageRepository.countByUserIdAndPromotionId(userId, promotion.getId());
            if (userUsageCount >= promotion.getMaxUsagePerUser()) {
                return PromotionValidationResponse.builder()
                        .valid(false)
                        .discount(0L)
                        .message("You have reached the per-user usage limit")
                        .build();
            }
        }

        if (promotion.getMinTransactionAmount() != null && amount < promotion.getMinTransactionAmount()) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .discount(0L)
                    .message("Minimum transaction amount is " + promotion.getMinTransactionAmount())
                    .build();
        }

        Long discount = calculateDiscount(promotion, amount);
        return PromotionValidationResponse.builder()
                .valid(true)
                .discount(discount)
                .message("Promotion applied successfully")
                .build();
    }

    @Override
    @Transactional
    public PromotionUsageResponse applyPromotion(UUID userId, ApplyPromotionRequest request, UUID transactionId) {
        Promotion promotion = promotionRepository.findByPromoCode(request.getPromoCode())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", request.getPromoCode()));

        PromotionValidationResponse validation = validatePromoCode(request.getPromoCode(), request.getTransactionAmount(), userId);
        if (!validation.isValid()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, validation.getMessage());
        }

        Long discount = validation.getDiscount();
        Long cashbackAmount = 0L;

        if (promotion.getType() == PromotionType.CASHBACK) {
            cashbackAmount = discount;
            discount = 0L;
        }

        PromotionUsage usage = PromotionUsage.builder()
                .promotionId(promotion.getId())
                .userId(userId)
                .transactionId(transactionId)
                .discountApplied(discount)
                .cashbackAmount(cashbackAmount)
                .build();
        usage = promotionUsageRepository.save(usage);

        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);

        if (cashbackAmount > 0) {
            creditCashbackWallet(userId, cashbackAmount, promotion.getId(), transactionId);
        }

        log.info("Promotion applied: promotionId={}, userId={}, discount={}, cashback={}",
                promotion.getId(), userId, discount, cashbackAmount);

        return PromotionUsageResponse.builder()
                .id(usage.getId())
                .promotionId(usage.getPromotionId())
                .userId(usage.getUserId())
                .transactionId(usage.getTransactionId())
                .discountApplied(usage.getDiscountApplied())
                .cashbackAmount(usage.getCashbackAmount())
                .createdAt(usage.getCreatedAt())
                .build();
    }

    @Override
    public CashbackWalletResponse getCashbackWallet(UUID userId) {
        CashbackWallet wallet = cashbackWalletRepository.findByUserId(userId)
                .orElseGet(() -> createCashbackWallet(userId));
        return mapWalletToResponse(wallet);
    }

    @Override
    @Transactional
    public CashbackWalletResponse redeemCashback(UUID userId, RedeemCashbackRequest request) {
        CashbackWallet wallet = cashbackWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cashback Wallet", userId.toString()));

        if (wallet.getBalance() < request.getAmount()) {
            throw new BusinessException(ErrorCodes.INSUFFICIENT_BALANCE,
                    "Insufficient cashback balance. Available: " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance() - request.getAmount());
        wallet.setTotalRedeemed(wallet.getTotalRedeemed() + request.getAmount());
        cashbackWalletRepository.save(wallet);

        CashbackTransaction redeemTxn = CashbackTransaction.builder()
                .cashbackWalletId(wallet.getId())
                .type(CashbackTxnType.REDEEMED)
                .amount(request.getAmount())
                .description("Cashback redeemed to main wallet")
                .build();
        cashbackTransactionRepository.save(redeemTxn);

        try {
            WebClient webClient = webClientBuilder.build();
            webClient.post()
                    .uri(WALLET_SERVICE_BASE + "/credit")
                    .bodyValue(java.util.Map.of(
                            "userId", userId.toString(),
                            "amount", request.getAmount(),
                            "description", "Cashback redemption",
                            "txnId", redeemTxn.getId().toString()
                    ))
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();
            log.info("Cashback redeemed: userId={}, amount={}", userId, request.getAmount());
        } catch (Exception e) {
            log.error("Failed to credit main wallet for cashback redemption: userId={}", userId, e);
        }

        return mapWalletToResponse(wallet);
    }

    @Override
    public Page<PromotionResponse> getMyPromotions(UUID userId, int page, int size) {
        Page<Promotion> promotions = promotionRepository.findByMerchantId(userId)
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, PageRequest.of(page, size), list.size())
                ));
        return promotions.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deactivatePromotion(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", promotionId.toString()));
        promotion.setStatus(PromotionStatus.PAUSED);
        promotionRepository.save(promotion);
        log.info("Promotion deactivated: id={}", promotionId);
    }

    private Long calculateDiscount(Promotion promotion, Long amount) {
        return switch (promotion.getType()) {
            case FIXED_DISCOUNT -> {
                Long discount = promotion.getDiscountValue();
                if (promotion.getMaxDiscount() != null && discount > promotion.getMaxDiscount()) {
                    discount = promotion.getMaxDiscount();
                }
                yield Math.min(discount, amount);
            }
            case PERCENTAGE_DISCOUNT -> {
                BigDecimal percentage = BigDecimal.valueOf(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
                Long discount = BigDecimal.valueOf(amount).multiply(percentage).setScale(0, RoundingMode.HALF_UP).longValue();
                if (promotion.getMaxDiscount() != null && discount > promotion.getMaxDiscount()) {
                    discount = promotion.getMaxDiscount();
                }
                yield Math.min(discount, amount);
            }
            case CASHBACK -> {
                BigDecimal percentage = BigDecimal.valueOf(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
                Long cashback = BigDecimal.valueOf(amount).multiply(percentage).setScale(0, RoundingMode.HALF_UP).longValue();
                if (promotion.getMaxDiscount() != null && cashback > promotion.getMaxDiscount()) {
                    cashback = promotion.getMaxDiscount();
                }
                yield cashback;
            }
            case COUPON_CODE -> {
                Long discount = promotion.getDiscountValue();
                if (promotion.getMaxDiscount() != null && discount > promotion.getMaxDiscount()) {
                    discount = promotion.getMaxDiscount();
                }
                yield Math.min(discount, amount);
            }
            default -> 0L;
        };
    }

    private void creditCashbackWallet(UUID userId, Long amount, UUID promotionId, UUID transactionId) {
        CashbackWallet wallet = cashbackWalletRepository.findByUserId(userId)
                .orElseGet(() -> createCashbackWallet(userId));

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setTotalEarned(wallet.getTotalEarned() + amount);
        cashbackWalletRepository.save(wallet);

        CashbackTransaction earnedTxn = CashbackTransaction.builder()
                .cashbackWalletId(wallet.getId())
                .type(CashbackTxnType.EARNED)
                .amount(amount)
                .promotionId(promotionId)
                .transactionId(transactionId)
                .description("Cashback earned from promotion")
                .build();
        cashbackTransactionRepository.save(earnedTxn);
    }

    private CashbackWallet createCashbackWallet(UUID userId) {
        CashbackWallet wallet = CashbackWallet.builder()
                .userId(userId)
                .balance(0L)
                .totalEarned(0L)
                .totalRedeemed(0L)
                .build();
        return cashbackWalletRepository.save(wallet);
    }

    private PromotionResponse mapToResponse(Promotion promotion) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean isActive = promotion.getStatus() == PromotionStatus.ACTIVE
                && now.isAfter(promotion.getStartDate())
                && now.isBefore(promotion.getEndDate());
        int remainingUses = promotion.getMaxUsageTotal() > 0
                ? promotion.getMaxUsageTotal() - promotion.getUsageCount()
                : -1;

        return PromotionResponse.builder()
                .id(promotion.getId())
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .type(promotion.getType())
                .fundingType(promotion.getFundingType())
                .merchantId(promotion.getMerchantId())
                .discountValue(promotion.getDiscountValue())
                .maxDiscount(promotion.getMaxDiscount())
                .minTransactionAmount(promotion.getMinTransactionAmount())
                .maxUsageTotal(promotion.getMaxUsageTotal())
                .maxUsagePerUser(promotion.getMaxUsagePerUser())
                .usageCount(promotion.getUsageCount())
                .remainingUses(remainingUses)
                .isActive(isActive)
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .status(promotion.getStatus())
                .promoCode(promotion.getPromoCode())
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .build();
    }

    private CashbackWalletResponse mapWalletToResponse(CashbackWallet wallet) {
        return CashbackWalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalRedeemed(wallet.getTotalRedeemed())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
