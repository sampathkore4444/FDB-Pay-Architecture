package com.fdbpay.referencedata.controller;

import com.fdbpay.shared.dto.ApiResponse;
import com.fdbpay.referencedata.dto.request.CreateReferenceTypeRequest;
import com.fdbpay.referencedata.dto.request.CreateReferenceValueRequest;
import com.fdbpay.referencedata.dto.request.UpdateReferenceTypeRequest;
import com.fdbpay.referencedata.dto.request.UpdateReferenceValueRequest;
import com.fdbpay.referencedata.dto.response.ReferenceDataLookupResponse;
import com.fdbpay.referencedata.dto.response.ReferenceTypeResponse;
import com.fdbpay.referencedata.dto.response.ReferenceTypeSummaryResponse;
import com.fdbpay.referencedata.dto.response.ReferenceValueResponse;
import com.fdbpay.referencedata.service.ReferenceDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/refdata")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReferenceTypeResponse> createType(@Valid @RequestBody CreateReferenceTypeRequest request) {
        return ApiResponse.success(referenceDataService.createType(request));
    }

    @GetMapping("/types")
    public ApiResponse<Page<ReferenceTypeSummaryResponse>> getAllTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(referenceDataService.getAllTypes(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))));
    }

    @GetMapping("/types/{id}")
    public ApiResponse<ReferenceTypeResponse> getType(@PathVariable UUID id) {
        return ApiResponse.success(referenceDataService.getType(id));
    }

    @PutMapping("/types/{id}")
    public ApiResponse<ReferenceTypeResponse> updateType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceTypeRequest request) {
        return ApiResponse.success(referenceDataService.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    public ApiResponse<Void> deleteType(@PathVariable UUID id) {
        referenceDataService.deleteType(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/types/{id}/values")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReferenceValueResponse> addValue(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReferenceValueRequest request) {
        return ApiResponse.success(referenceDataService.addValue(id, request));
    }

    @GetMapping("/values/{id}")
    public ApiResponse<ReferenceValueResponse> getValue(@PathVariable UUID id) {
        return ApiResponse.success(referenceDataService.getValue(id));
    }

    @PutMapping("/values/{id}")
    public ApiResponse<ReferenceValueResponse> updateValue(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferenceValueRequest request) {
        return ApiResponse.success(referenceDataService.updateValue(id, request));
    }

    @DeleteMapping("/values/{id}")
    public ApiResponse<Void> deleteValue(@PathVariable UUID id) {
        referenceDataService.deleteValue(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/type/{code}")
    public ApiResponse<ReferenceDataLookupResponse> getLookup(@PathVariable String code) {
        return ApiResponse.success(referenceDataService.getLookup(code));
    }
}
