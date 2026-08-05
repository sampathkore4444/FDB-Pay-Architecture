package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.merchant.service.client.AuthServiceClient;
import com.fdbpay.merchant.service.dto.request.AddStaffRequest;
import com.fdbpay.merchant.service.dto.response.StaffAccountResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.StaffAccount;
import com.fdbpay.merchant.service.model.enums.StaffAccountStatus;
import com.fdbpay.merchant.service.model.enums.StaffRole;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.StaffAccountRepository;
import com.fdbpay.merchant.service.service.StaffService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffAccountRepository staffAccountRepository;
    private final MerchantRepository merchantRepository;
    private final AuthServiceClient authServiceClient;

    private static final Map<StaffRole, List<String>> DEFAULT_PERMISSIONS = Map.of(
            StaffRole.OWNER, List.of("terminal", "refunds", "reports", "links", "settlements", "staff"),
            StaffRole.MANAGER, List.of("terminal", "refunds", "reports", "links", "settlements"),
            StaffRole.CASHIER, List.of("terminal"),
            StaffRole.VIEWER, List.of("reports"));

    @Override
    @Transactional
    public StaffAccountResponse addStaff(UUID merchantId, UUID userId, AddStaffRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        if (!merchant.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the merchant owner can add staff");
        }

        staffAccountRepository.findByMerchantIdAndUserId(merchantId, request.getUserId())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "User is already staff for this merchant");
                });

        StaffAccount staff = StaffAccount.builder()
                .merchantId(merchantId)
                .userId(request.getUserId())
                .role(request.getRole())
                .status(StaffAccountStatus.ACTIVE)
                .dailyLimit(request.getDailyLimit())
                .storeId(request.getStoreId())
                .permissions(resolvePermissions(request.getRole(), request.getPermissions()))
                .build();

        staff = staffAccountRepository.save(staff);
        log.info("Staff added: staffId={}, merchantId={}, userId={}, role={}",
                staff.getId(), merchantId, request.getUserId(), request.getRole());

        return mapToResponse(staff);
    }

    @Override
    @Transactional
    public void removeStaff(UUID staffId, UUID merchantId) {
        StaffAccount staff = staffAccountRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffAccount", staffId.toString()));

        if (!staff.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Staff does not belong to this merchant");
        }

        if (staff.getRole() == StaffRole.OWNER) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Cannot remove the owner");
        }

        staff.setStatus(StaffAccountStatus.INACTIVE);
        staffAccountRepository.save(staff);
        log.info("Staff removed: staffId={}, merchantId={}", staffId, merchantId);
    }

    @Override
    public List<StaffAccountResponse> getStaffByMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        return staffAccountRepository.findByMerchantId(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StaffAccountResponse updateStaffRole(UUID staffId, StaffRole role, UUID merchantId) {
        StaffAccount staff = staffAccountRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffAccount", staffId.toString()));

        if (!staff.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Staff does not belong to this merchant");
        }

        staff.setRole(role);
        staff.setPermissions(resolvePermissions(role, staff.getPermissions() != null
                ? parsePermissions(staff.getPermissions()) : null));
        staff = staffAccountRepository.save(staff);
        log.info("Staff role updated: staffId={}, newRole={}", staffId, role);

        return mapToResponse(staff);
    }

    @Override
    @Transactional
    public StaffAccountResponse updateStaffPermissions(UUID staffId, UUID merchantId, List<String> permissions) {
        StaffAccount staff = staffAccountRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffAccount", staffId.toString()));

        if (!staff.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Staff does not belong to this merchant");
        }

        staff.setPermissions(String.join(",", permissions));
        staff = staffAccountRepository.save(staff);
        log.info("Staff permissions updated: staffId={}", staffId);
        return mapToResponse(staff);
    }

    private String resolvePermissions(StaffRole role, List<String> requested) {
        if (requested != null && !requested.isEmpty()) {
            return String.join(",", requested);
        }
        return String.join(",", DEFAULT_PERMISSIONS.getOrDefault(role, List.of()));
    }

    private List<String> parsePermissions(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private StaffAccountResponse mapToResponse(StaffAccount staff) {
        Map<String, String> user = authServiceClient.getUser(staff.getUserId());
        return StaffAccountResponse.builder()
                .id(staff.getId())
                .merchantId(staff.getMerchantId())
                .userId(staff.getUserId())
                .userName(user.getOrDefault("name", ""))
                .userPhone(user.getOrDefault("phone", ""))
                .role(staff.getRole())
                .status(staff.getStatus())
                .dailyLimit(staff.getDailyLimit())
                .storeId(staff.getStoreId())
                .permissions(parsePermissions(staff.getPermissions()))
                .createdAt(staff.getCreatedAt())
                .build();
    }
}
