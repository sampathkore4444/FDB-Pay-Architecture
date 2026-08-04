package com.fdbpay.referencedata.service;

import com.fdbpay.referencedata.dto.request.CreateReferenceTypeRequest;
import com.fdbpay.referencedata.dto.request.CreateReferenceValueRequest;
import com.fdbpay.referencedata.dto.request.UpdateReferenceTypeRequest;
import com.fdbpay.referencedata.dto.request.UpdateReferenceValueRequest;
import com.fdbpay.referencedata.dto.response.ReferenceDataLookupResponse;
import com.fdbpay.referencedata.dto.response.ReferenceTypeResponse;
import com.fdbpay.referencedata.dto.response.ReferenceTypeSummaryResponse;
import com.fdbpay.referencedata.dto.response.ReferenceValueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReferenceDataService {

    ReferenceTypeResponse createType(CreateReferenceTypeRequest request);

    Page<ReferenceTypeSummaryResponse> getAllTypes(Pageable pageable);

    ReferenceTypeResponse getType(UUID id);

    ReferenceTypeResponse updateType(UUID id, UpdateReferenceTypeRequest request);

    void deleteType(UUID id);

    ReferenceValueResponse addValue(UUID typeId, CreateReferenceValueRequest request);

    ReferenceValueResponse getValue(UUID valueId);

    ReferenceValueResponse updateValue(UUID valueId, UpdateReferenceValueRequest request);

    void deleteValue(UUID valueId);

    ReferenceDataLookupResponse getLookup(String code);
}
