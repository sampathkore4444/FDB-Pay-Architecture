package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.dto.request.AddStaffRequest;
import com.fdbpay.merchant.service.dto.response.StaffAccountResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.StaffAccount;
import com.fdbpay.merchant.service.model.enums.StaffAccountStatus;
import com.fdbpay.merchant.service.model.enums.StaffRole;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.StaffAccountRepository;
import com.fdbpay.merchant.service.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffAccountRepository staffAccountRepository;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public StaffAccountResponse addStaff(UUID merchantId, AddStaffRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        boolean hasOwner = staffAccountRepository.findByMerchantIdAndStatus(merchantId, StaffAccountStatus.ACTIVE)
                .stream()
                .anyMatch(s -> s.getRole() == StaffRole.OWNER && s.getUserId().equals(request.getUserId()));

        if (!hasOwner) {
            boolean userIsMerchantOwner = merchant.getUserId().equals(request.getUserId());
            if (!userIsMerchantOwner) {
                throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the merchant owner can add staff");
            }
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
        staff = staffAccountRepository.save(staff);
        log.info("Staff role updated: staffId={}, newRole={}", staffId, role);

        return mapToResponse(staff);
    }

    private StaffAccountResponse mapToResponse(StaffAccount staff) {
        return StaffAccountResponse.builder()
                .id(staff.getId())
                .merchantId(staff.getMerchantId())
                .userId(staff.getUserId())
                .role(staff.getRole())
                .status(staff.getStatus())
                .dailyLimit(staff.getDailyLimit())
                .createdAt(staff.getCreatedAt())
                .build();
    }
}
