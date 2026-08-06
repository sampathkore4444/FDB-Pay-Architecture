package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.CashbackCampaignRequest;
import com.fdbpay.merchant.service.dto.request.AbTestRequest;
import com.fdbpay.merchant.service.dto.request.DiscountCodeRequest;
import com.fdbpay.merchant.service.dto.request.LoyaltySettingsRequest;
import com.fdbpay.merchant.service.dto.request.MarketingCampaignRequest;
import com.fdbpay.merchant.service.dto.request.ReferralProgramRequest;
import com.fdbpay.merchant.service.dto.response.CashbackCampaignResponse;
import com.fdbpay.merchant.service.dto.response.DiscountCodeResponse;
import com.fdbpay.merchant.service.dto.response.LoyaltySettingsResponse;
import com.fdbpay.merchant.service.dto.response.MarketingCampaignResponse;
import com.fdbpay.merchant.service.dto.response.ReferralProgramResponse;
import com.fdbpay.merchant.service.model.CashbackCampaign;
import com.fdbpay.merchant.service.model.DiscountCode;
import com.fdbpay.merchant.service.model.LoyaltySettings;
import com.fdbpay.merchant.service.model.MarketingCampaign;
import com.fdbpay.merchant.service.model.ReferralProgram;
import com.fdbpay.merchant.service.model.enums.ActiveStatus;
import com.fdbpay.merchant.service.model.enums.CampaignType;
import com.fdbpay.merchant.service.model.enums.DiscountType;
import com.fdbpay.merchant.service.repository.CashbackCampaignRepository;
import com.fdbpay.merchant.service.repository.DiscountCodeRepository;
import com.fdbpay.merchant.service.repository.LoyaltySettingsRepository;
import com.fdbpay.merchant.service.repository.MarketingCampaignRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.ReferralProgramRepository;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingService {

    private final DiscountCodeRepository discountCodeRepository;
    private final CashbackCampaignRepository cashbackCampaignRepository;
    private final ReferralProgramRepository referralProgramRepository;
    private final LoyaltySettingsRepository loyaltySettingsRepository;
    private final MarketingCampaignRepository marketingCampaignRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;
    private final Random random = new Random();

    // ---- A/B discount-code variants ----

    @Transactional
    public List<DiscountCodeResponse> createAbTest(UUID merchantId, AbTestRequest request) {
        requireMerchant(merchantId);
        if (request.getVariants() == null || request.getVariants().size() < 2) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "An A/B test requires at least 2 variants");
        }
        List<DiscountCodeResponse> created = new java.util.ArrayList<>();
        for (AbTestRequest.AbVariantRequest variant : request.getVariants()) {
            DiscountCode code = DiscountCode.builder()
                    .merchantId(merchantId)
                    .code(variant.getCode().trim().toUpperCase(Locale.ROOT))
                    .type(java.util.Arrays.stream(DiscountType.values())
                            .filter(t -> t.name().equalsIgnoreCase(variant.getType()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException(ErrorCodes.VALIDATION_ERROR, "Unsupported discount type: " + variant.getType())))
                    .value(variant.getValue())
                    .minSpend(request.getMinSpend() == null ? 0L : request.getMinSpend())
                    .maxUses(request.getMaxUses())
                    .usedCount(0)
                    .validFrom(OffsetDateTime.now())
                    .validTo(request.getValidTo())
                    .status(ActiveStatus.ACTIVE)
                    .build();
            code = discountCodeRepository.save(code);
            created.add(mapDiscount(code));
        }
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "AB_TEST", merchantId.toString(),
                "Launched A/B test '" + request.getName() + "' with " + created.size() + " variants");
        return created;
    }

    // ---- Automated marketing campaigns ----

    public List<MarketingCampaignResponse> listMarketingCampaigns(UUID merchantId) {
        requireMerchant(merchantId);
        return marketingCampaignRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapCampaign2).toList();
    }

    @Transactional
    public MarketingCampaignResponse createMarketingCampaign(UUID merchantId, MarketingCampaignRequest request) {
        requireMerchant(merchantId);
        CampaignType type = parseCampaignType(request.getCampaignType());
        MarketingCampaign campaign = MarketingCampaign.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .campaignType(type)
                .audienceSegment(request.getAudienceSegment())
                .discountCodeId(request.getDiscountCodeId())
                .cashbackId(request.getCashbackId())
                .status(ActiveStatus.ACTIVE)
                .build();
        campaign = marketingCampaignRepository.save(campaign);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "MARKETING_CAMPAIGN", campaign.getId().toString(),
                "Created automated campaign '" + campaign.getName() + "' targeting " + campaign.getAudienceSegment());
        return mapCampaign2(campaign);
    }

    @Transactional
    public MarketingCampaignResponse toggleMarketingCampaign(UUID merchantId, UUID campaignId) {
        MarketingCampaign campaign = getOwnedCampaign2(merchantId, campaignId);
        campaign.setStatus(campaign.getStatus() == ActiveStatus.ACTIVE ? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE);
        campaign = marketingCampaignRepository.save(campaign);
        return mapCampaign2(campaign);
    }

    @Transactional
    public void deleteMarketingCampaign(UUID merchantId, UUID campaignId) {
        MarketingCampaign campaign = getOwnedCampaign2(merchantId, campaignId);
        marketingCampaignRepository.delete(campaign);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "MARKETING_CAMPAIGN", campaignId.toString(),
                "Deleted automated campaign '" + campaign.getName() + "'");
    }

    private CampaignType parseCampaignType(String raw) {
        try {
            return CampaignType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Unsupported campaign type: " + raw);
        }
    }

    private MarketingCampaign getOwnedCampaign2(UUID merchantId, UUID campaignId) {
        MarketingCampaign campaign = marketingCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketingCampaign", campaignId.toString()));
        if (!campaign.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Campaign does not belong to this merchant");
        }
        return campaign;
    }

    private MarketingCampaignResponse mapCampaign2(MarketingCampaign campaign) {
        return MarketingCampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .campaignType(campaign.getCampaignType())
                .audienceSegment(campaign.getAudienceSegment())
                .discountCodeId(campaign.getDiscountCodeId())
                .cashbackId(campaign.getCashbackId())
                .status(campaign.getStatus())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    // ---- Discount codes ----

    public List<DiscountCodeResponse> listDiscountCodes(UUID merchantId) {
        requireMerchant(merchantId);
        return discountCodeRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapDiscount).toList();
    }

    @Transactional
    public DiscountCodeResponse createDiscountCode(UUID merchantId, DiscountCodeRequest request) {
        requireMerchant(merchantId);
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        discountCodeRepository.findByMerchantIdAndCodeIgnoreCase(merchantId, code)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Discount code already exists: " + code);
                });
        DiscountCode entity = DiscountCode.builder()
                .merchantId(merchantId)
                .code(code)
                .type(request.getType())
                .value(request.getValue())
                .minSpend(request.getMinSpend() == null ? 0L : request.getMinSpend())
                .maxUses(request.getMaxUses())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .usedCount(0)
                .status(ActiveStatus.ACTIVE)
                .build();
        entity = discountCodeRepository.save(entity);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "DISCOUNT_CODE", entity.getId().toString(), "Created discount code " + code);
        return mapDiscount(entity);
    }

    @Transactional
    public DiscountCodeResponse toggleDiscountCode(UUID merchantId, UUID codeId) {
        DiscountCode code = getOwnedDiscount(merchantId, codeId);
        code.setStatus(code.getStatus() == ActiveStatus.ACTIVE ? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE);
        code = discountCodeRepository.save(code);
        return mapDiscount(code);
    }

    @Transactional
    public void deleteDiscountCode(UUID merchantId, UUID codeId) {
        DiscountCode code = getOwnedDiscount(merchantId, codeId);
        discountCodeRepository.delete(code);
        auditService.log(merchantId, "OWNER", null, null, "DELETE", "DISCOUNT_CODE", codeId.toString(), "Deleted discount code " + code.getCode());
    }

    public DiscountCodeResponse validateDiscountCode(UUID merchantId, String code, Long amount) {
        DiscountCode entity = discountCodeRepository.findByMerchantIdAndCodeIgnoreCase(merchantId, code.trim())
                .orElseThrow(() -> new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid discount code"));
        if (entity.getStatus() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Discount code is inactive");
        }
        if (amount != null && amount < entity.getMinSpend()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Minimum spend of " + entity.getMinSpend() + " MMK required");
        }
        if (entity.getMaxUses() != null && entity.getUsedCount() >= entity.getMaxUses()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Discount code usage limit reached");
        }
        if (entity.getValidFrom() != null && OffsetDateTime.now().isBefore(entity.getValidFrom())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Discount code is not yet valid");
        }
        if (entity.getValidTo() != null && OffsetDateTime.now().isAfter(entity.getValidTo())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Discount code has expired");
        }
        return mapDiscount(entity);
    }

    @Transactional
    public DiscountCodeResponse useDiscountCode(UUID merchantId, String code) {
        DiscountCode entity = getOwnedDiscount(merchantId, codeIdFromCode(merchantId, code));
        entity.setUsedCount(entity.getUsedCount() + 1);
        entity = discountCodeRepository.save(entity);
        return mapDiscount(entity);
    }

    private UUID codeIdFromCode(UUID merchantId, String code) {
        return discountCodeRepository.findByMerchantIdAndCodeIgnoreCase(merchantId, code)
                .orElseThrow(() -> new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid discount code")).getId();
    }

    // ---- Cashback campaigns ----

    public List<CashbackCampaignResponse> listCampaigns(UUID merchantId) {
        requireMerchant(merchantId);
        return cashbackCampaignRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapCampaign).toList();
    }

    @Transactional
    public CashbackCampaignResponse createCampaign(UUID merchantId, CashbackCampaignRequest request) {
        requireMerchant(merchantId);
        CashbackCampaign campaign = CashbackCampaign.builder()
                .merchantId(merchantId)
                .name(request.getName())
                .percent(request.getPercent())
                .budget(request.getBudget())
                .spent(request.getSpent() == null ? 0L : request.getSpent())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .status(ActiveStatus.ACTIVE)
                .build();
        campaign = cashbackCampaignRepository.save(campaign);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "CASHBACK_CAMPAIGN", campaign.getId().toString(),
                "Created cashback campaign '" + campaign.getName() + "' (" + campaign.getPercent() + "%)");
        return mapCampaign(campaign);
    }

    @Transactional
    public CashbackCampaignResponse toggleCampaign(UUID merchantId, UUID campaignId) {
        CashbackCampaign campaign = getOwnedCampaign(merchantId, campaignId);
        campaign.setStatus(campaign.getStatus() == ActiveStatus.ACTIVE ? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE);
        campaign = cashbackCampaignRepository.save(campaign);
        return mapCampaign(campaign);
    }

    @Transactional
    public void deleteCampaign(UUID merchantId, UUID campaignId) {
        CashbackCampaign campaign = getOwnedCampaign(merchantId, campaignId);
        cashbackCampaignRepository.delete(campaign);
    }

    // ---- Referral programs ----

    public List<ReferralProgramResponse> listReferralPrograms(UUID merchantId) {
        requireMerchant(merchantId);
        return referralProgramRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream().map(this::mapReferral).toList();
    }

    @Transactional
    public ReferralProgramResponse createReferralProgram(UUID merchantId, ReferralProgramRequest request) {
        requireMerchant(merchantId);
        ReferralProgram program = ReferralProgram.builder()
                .merchantId(merchantId)
                .code(request.getCode().trim().toUpperCase(Locale.ROOT))
                .referralBonus(request.getReferralBonus() == null ? 0L : request.getReferralBonus())
                .referredBonus(request.getReferredBonus() == null ? 0L : request.getReferredBonus())
                .uses(0)
                .status(ActiveStatus.ACTIVE)
                .build();
        program = referralProgramRepository.save(program);
        auditService.log(merchantId, "OWNER", null, null, "CREATE", "REFERRAL_PROGRAM", program.getId().toString(),
                "Created referral program with code " + program.getCode());
        return mapReferral(program);
    }

    @Transactional
    public ReferralProgramResponse toggleReferralProgram(UUID merchantId, UUID programId) {
        ReferralProgram program = getOwnedReferral(merchantId, programId);
        program.setStatus(program.getStatus() == ActiveStatus.ACTIVE ? ActiveStatus.INACTIVE : ActiveStatus.ACTIVE);
        program = referralProgramRepository.save(program);
        return mapReferral(program);
    }

    @Transactional
    public void deleteReferralProgram(UUID merchantId, UUID programId) {
        ReferralProgram program = getOwnedReferral(merchantId, programId);
        referralProgramRepository.delete(program);
    }

    public String generateReferralCode() {
        return "REF-" + (100000 + random.nextInt(900000));
    }

    // ---- Loyalty ----

    public LoyaltySettingsResponse getLoyaltySettings(UUID merchantId) {
        requireMerchant(merchantId);
        LoyaltySettings settings = loyaltySettingsRepository.findByMerchantId(merchantId)
                .orElseGet(() -> loyaltySettingsRepository.save(LoyaltySettings.builder().merchantId(merchantId).build()));
        return mapLoyalty(settings);
    }

    @Transactional
    public LoyaltySettingsResponse updateLoyaltySettings(UUID merchantId, LoyaltySettingsRequest request) {
        requireMerchant(merchantId);
        LoyaltySettings settings = loyaltySettingsRepository.findByMerchantId(merchantId)
                .orElseGet(() -> LoyaltySettings.builder().merchantId(merchantId).build());
        settings.setPointsPerMmk(request.getPointsPerMmk());
        settings.setRewardThresholdPoints(request.getRewardThresholdPoints());
        settings.setRewardValue(request.getRewardValue());
        if (request.getEnabled() != null) {
            settings.setEnabled(request.getEnabled());
        }
        settings = loyaltySettingsRepository.save(settings);
        auditService.log(merchantId, "OWNER", null, null, "UPDATE", "LOYALTY_SETTINGS", merchantId.toString(), "Updated loyalty program settings");
        return mapLoyalty(settings);
    }

    private DiscountCode getOwnedDiscount(UUID merchantId, UUID codeId) {
        DiscountCode code = discountCodeRepository.findById(codeId)
                .orElseThrow(() -> new ResourceNotFoundException("DiscountCode", codeId.toString()));
        if (!code.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Discount code does not belong to this merchant");
        }
        return code;
    }

    private CashbackCampaign getOwnedCampaign(UUID merchantId, UUID campaignId) {
        CashbackCampaign campaign = cashbackCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("CashbackCampaign", campaignId.toString()));
        if (!campaign.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Campaign does not belong to this merchant");
        }
        return campaign;
    }

    private ReferralProgram getOwnedReferral(UUID merchantId, UUID programId) {
        ReferralProgram program = referralProgramRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("ReferralProgram", programId.toString()));
        if (!program.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Referral program does not belong to this merchant");
        }
        return program;
    }

    private void requireMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
    }

    private DiscountCodeResponse mapDiscount(DiscountCode code) {
        return DiscountCodeResponse.builder()
                .id(code.getId())
                .merchantId(code.getMerchantId())
                .code(code.getCode())
                .type(code.getType())
                .value(code.getValue())
                .minSpend(code.getMinSpend())
                .maxUses(code.getMaxUses())
                .usedCount(code.getUsedCount())
                .validFrom(code.getValidFrom())
                .validTo(code.getValidTo())
                .status(code.getStatus())
                .createdAt(code.getCreatedAt())
                .build();
    }

    private CashbackCampaignResponse mapCampaign(CashbackCampaign campaign) {
        return CashbackCampaignResponse.builder()
                .id(campaign.getId())
                .merchantId(campaign.getMerchantId())
                .name(campaign.getName())
                .percent(campaign.getPercent())
                .budget(campaign.getBudget())
                .spent(campaign.getSpent())
                .startsAt(campaign.getStartsAt())
                .endsAt(campaign.getEndsAt())
                .status(campaign.getStatus())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    private ReferralProgramResponse mapReferral(ReferralProgram program) {
        return ReferralProgramResponse.builder()
                .id(program.getId())
                .merchantId(program.getMerchantId())
                .code(program.getCode())
                .referralBonus(program.getReferralBonus())
                .referredBonus(program.getReferredBonus())
                .uses(program.getUses())
                .status(program.getStatus())
                .createdAt(program.getCreatedAt())
                .build();
    }

    private LoyaltySettingsResponse mapLoyalty(LoyaltySettings settings) {
        return LoyaltySettingsResponse.builder()
                .enabled(settings.getEnabled())
                .pointsPerMmk(settings.getPointsPerMmk())
                .rewardThresholdPoints(settings.getRewardThresholdPoints())
                .rewardValue(settings.getRewardValue())
                .build();
    }
}
