package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.merchant.service.dto.request.AddChargebackNoteRequest;
import com.fdbpay.merchant.service.dto.request.AddChargebackRequest;
import com.fdbpay.merchant.service.dto.request.RespondChargebackRequest;
import com.fdbpay.merchant.service.dto.response.ChargebackNoteResponse;
import com.fdbpay.merchant.service.dto.response.ChargebackResponse;
import com.fdbpay.merchant.service.model.Chargeback;
import com.fdbpay.merchant.service.model.ChargebackNote;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.enums.ChargebackStatus;
import com.fdbpay.merchant.service.repository.ChargebackNoteRepository;
import com.fdbpay.merchant.service.repository.ChargebackRepository;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.service.ChargebackService;
import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargebackServiceImpl implements ChargebackService {

    private final ChargebackRepository chargebackRepository;
    private final ChargebackNoteRepository noteRepository;
    private final MerchantRepository merchantRepository;

    @Override
    public List<ChargebackResponse> getByMerchant(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));
        return chargebackRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ChargebackResponse getDetail(UUID merchantId, UUID chargebackId) {
        Chargeback chargeback = findOwned(merchantId, chargebackId);
        return mapToResponse(chargeback);
    }

    @Override
    @Transactional
    public ChargebackResponse openChargeback(UUID merchantId, AddChargebackRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Chargeback chargeback = Chargeback.builder()
                .merchantId(merchant.getId())
                .transactionId(request.getTransactionId())
                .amount(request.getAmount())
                .reasonCode(request.getReasonCode())
                .customerNotes(request.getCustomerNotes())
                .deadline(request.getDeadline() != null ? request.getDeadline() : OffsetDateTime.now().plusDays(14))
                .status(ChargebackStatus.OPEN)
                .build();
        chargeback = chargebackRepository.save(chargeback);
        log.info("Chargeback opened: id={}, merchantId={}, amount={}", chargeback.getId(), merchantId, request.getAmount());
        return mapToResponse(chargeback);
    }

    @Override
    @Transactional
    public ChargebackResponse addNote(UUID merchantId, UUID chargebackId, AddChargebackNoteRequest request) {
        Chargeback chargeback = findOwned(merchantId, chargebackId);
        saveNote(chargeback.getId(), "MERCHANT", request.getAuthorName(), request.getMessage());
        return mapToResponse(chargeback);
    }

    @Override
    @Transactional
    public ChargebackResponse respond(UUID merchantId, UUID chargebackId, RespondChargebackRequest request) {
        Chargeback chargeback = findOwned(merchantId, chargebackId);
        if (chargeback.getStatus() == ChargebackStatus.WON
                || chargeback.getStatus() == ChargebackStatus.LOST
                || chargeback.getStatus() == ChargebackStatus.CLOSED) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Chargeback is already finalised");
        }
        chargeback.setStatus(request.getStatus());
        chargeback.setUpdatedAt(OffsetDateTime.now());
        chargeback = chargebackRepository.save(chargeback);

        if (request.getNote() != null && !request.getNote().isBlank()) {
            saveNote(chargeback.getId(), "MERCHANT", null,
                    "Status changed to " + request.getStatus() + " - " + request.getNote());
        }
        log.info("Chargeback responded: id={}, status={}", chargebackId, request.getStatus());
        return mapToResponse(chargeback);
    }

    private Chargeback findOwned(UUID merchantId, UUID chargebackId) {
        Chargeback chargeback = chargebackRepository.findById(chargebackId)
                .orElseThrow(() -> new ResourceNotFoundException("Chargeback", chargebackId.toString()));
        if (!chargeback.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Chargeback does not belong to this merchant");
        }
        return chargeback;
    }

    private void saveNote(UUID chargebackId, String authorType, String authorName, String message) {
        noteRepository.save(ChargebackNote.builder()
                .chargebackId(chargebackId)
                .authorType(authorType)
                .authorName(authorName)
                .message(message)
                .build());
    }

    private ChargebackResponse mapToResponse(Chargeback chargeback) {
        List<ChargebackNoteResponse> notes = noteRepository.findByChargebackIdOrderByCreatedAtAsc(chargeback.getId())
                .stream()
                .map(n -> ChargebackNoteResponse.builder()
                        .id(n.getId())
                        .authorType(n.getAuthorType())
                        .authorName(n.getAuthorName())
                        .message(n.getMessage())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
        return ChargebackResponse.builder()
                .id(chargeback.getId())
                .merchantId(chargeback.getMerchantId())
                .transactionId(chargeback.getTransactionId())
                .amount(chargeback.getAmount())
                .currency(chargeback.getCurrency())
                .reasonCode(chargeback.getReasonCode())
                .status(chargeback.getStatus())
                .customerNotes(chargeback.getCustomerNotes())
                .deadline(chargeback.getDeadline())
                .createdAt(chargeback.getCreatedAt())
                .updatedAt(chargeback.getUpdatedAt())
                .notes(notes)
                .build();
    }
}
