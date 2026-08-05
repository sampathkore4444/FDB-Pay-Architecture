package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.AddStaffRequest;
import com.fdbpay.merchant.service.dto.response.StaffAccountResponse;
import com.fdbpay.merchant.service.model.enums.StaffRole;

import java.util.List;
import java.util.UUID;

public interface StaffService {

    StaffAccountResponse addStaff(UUID merchantId, UUID userId, AddStaffRequest request);

    void removeStaff(UUID staffId, UUID merchantId);

    List<StaffAccountResponse> getStaffByMerchant(UUID merchantId);

    StaffAccountResponse updateStaffRole(UUID staffId, StaffRole role, UUID merchantId);

    StaffAccountResponse updateStaffPermissions(UUID staffId, UUID merchantId, List<String> permissions);
}
