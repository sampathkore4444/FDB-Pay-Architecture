package com.fdbpay.referencedata.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.referencedata.dto.request.CreateReferenceTypeRequest;
import com.fdbpay.referencedata.dto.request.CreateReferenceValueRequest;
import com.fdbpay.referencedata.dto.request.UpdateReferenceTypeRequest;
import com.fdbpay.referencedata.dto.request.UpdateReferenceValueRequest;
import com.fdbpay.referencedata.dto.response.ReferenceDataLookupResponse;
import com.fdbpay.referencedata.dto.response.ReferenceTypeResponse;
import com.fdbpay.referencedata.dto.response.ReferenceTypeSummaryResponse;
import com.fdbpay.referencedata.dto.response.ReferenceValueResponse;
import com.fdbpay.referencedata.model.ReferenceType;
import com.fdbpay.referencedata.model.ReferenceValue;
import com.fdbpay.referencedata.repository.ReferenceTypeRepository;
import com.fdbpay.referencedata.repository.ReferenceValueRepository;
import com.fdbpay.referencedata.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceDataServiceImpl implements ReferenceDataService {

    private final ReferenceTypeRepository typeRepository;
    private final ReferenceValueRepository valueRepository;

    @Override
    @Transactional
    public ReferenceTypeResponse createType(CreateReferenceTypeRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (typeRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Reference type with code " + code + " already exists");
        }

        ReferenceType type = ReferenceType.builder()
                .code(code)
                .description(request.getDescription().trim())
                .active(request.getActive() == null || request.getActive())
                .build();

        type = typeRepository.save(type);
        log.info("Reference type created: id={}, code={}", type.getId(), type.getCode());
        return mapToTypeResponse(type);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReferenceTypeSummaryResponse> getAllTypes(Pageable pageable) {
        Page<ReferenceType> types = typeRepository.findAll(pageable);

        Map<UUID, Long> valueCounts = typeRepository.countValuesGroupedByType().stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return types.map(t -> ReferenceTypeSummaryResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .description(t.getDescription())
                .active(t.getActive())
                .valueCount(valueCounts.getOrDefault(t.getId(), 0L))
                .updatedAt(t.getUpdatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceTypeResponse getType(UUID id) {
        ReferenceType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceType", id.toString()));
        return mapToTypeResponse(type);
    }

    @Override
    @Transactional
    public ReferenceTypeResponse updateType(UUID id, UpdateReferenceTypeRequest request) {
        ReferenceType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceType", id.toString()));

        type.setDescription(request.getDescription().trim());
        if (request.getActive() != null) {
            type.setActive(request.getActive());
        }
        type = typeRepository.save(type);
        log.info("Reference type updated: id={}, code={}", type.getId(), type.getCode());
        return mapToTypeResponse(type);
    }

    @Override
    @Transactional
    public void deleteType(UUID id) {
        if (!typeRepository.existsById(id)) {
            throw new ResourceNotFoundException("ReferenceType", id.toString());
        }
        typeRepository.deleteById(id);
        log.info("Reference type deleted: id={}", id);
    }

    @Override
    @Transactional
    public ReferenceValueResponse addValue(UUID typeId, CreateReferenceValueRequest request) {
        ReferenceType type = typeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceType", typeId.toString()));

        String code = request.getCode().trim().toUpperCase();
        if (valueRepository.existsByTypeIdAndCode(typeId, code)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Value with code " + code + " already exists in this type");
        }

        ReferenceValue value = ReferenceValue.builder()
                .type(type)
                .value(request.getValue().trim())
                .code(code)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() == null || request.getActive())
                .build();

        value = valueRepository.save(value);
        log.info("Reference value created: id={}, typeId={}, code={}", value.getId(), typeId, value.getCode());
        return mapToValueResponse(value);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceValueResponse getValue(UUID valueId) {
        ReferenceValue value = valueRepository.findById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceValue", valueId.toString()));
        return mapToValueResponse(value);
    }

    @Override
    @Transactional
    public ReferenceValueResponse updateValue(UUID valueId, UpdateReferenceValueRequest request) {
        ReferenceValue value = valueRepository.findById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceValue", valueId.toString()));

        String code = request.getCode().trim().toUpperCase();
        if (valueRepository.existsByTypeIdAndCodeAndIdNot(value.getType().getId(), code, valueId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Value with code " + code + " already exists in this type");
        }

        value.setValue(request.getValue().trim());
        value.setCode(code);
        if (request.getSortOrder() != null) {
            value.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            value.setActive(request.getActive());
        }
        value = valueRepository.save(value);
        log.info("Reference value updated: id={}, code={}", value.getId(), value.getCode());
        return mapToValueResponse(value);
    }

    @Override
    @Transactional
    public void deleteValue(UUID valueId) {
        if (!valueRepository.existsById(valueId)) {
            throw new ResourceNotFoundException("ReferenceValue", valueId.toString());
        }
        valueRepository.deleteById(valueId);
        log.info("Reference value deleted: id={}", valueId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceDataLookupResponse getLookup(String code) {
        ReferenceType type = typeRepository.findWithValuesByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("ReferenceType", code.toUpperCase()));

        return ReferenceDataLookupResponse.builder()
                .code(type.getCode())
                .description(type.getDescription())
                .values(type.getValues() == null ? Collections.emptyList()
                        : type.getValues().stream()
                                .filter(ReferenceValue::getActive)
                                .map(v -> ReferenceDataLookupResponse.LookupValue.builder()
                                        .id(v.getId())
                                        .value(v.getValue())
                                        .code(v.getCode())
                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    private ReferenceTypeResponse mapToTypeResponse(ReferenceType type) {
        return ReferenceTypeResponse.builder()
                .id(type.getId())
                .code(type.getCode())
                .description(type.getDescription())
                .active(type.getActive())
                .values(type.getValues() == null ? Collections.emptyList()
                        : type.getValues().stream().map(this::mapToValueResponse).collect(Collectors.toList()))
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }

    private ReferenceValueResponse mapToValueResponse(ReferenceValue value) {
        return ReferenceValueResponse.builder()
                .id(value.getId())
                .value(value.getValue())
                .code(value.getCode())
                .sortOrder(value.getSortOrder())
                .active(value.getActive())
                .build();
    }
}
