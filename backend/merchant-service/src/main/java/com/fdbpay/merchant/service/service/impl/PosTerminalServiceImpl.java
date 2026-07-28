package com.fdbpay.merchant.service.service.impl;

import com.fdbpay.shared.constants.ErrorCodes;
import com.fdbpay.shared.exceptions.BusinessException;
import com.fdbpay.shared.exceptions.ResourceNotFoundException;
import com.fdbpay.merchant.service.dto.request.CreatePosTerminalRequest;
import com.fdbpay.merchant.service.dto.response.PosTerminalResponse;
import com.fdbpay.merchant.service.model.Merchant;
import com.fdbpay.merchant.service.model.PosTerminal;
import com.fdbpay.merchant.service.model.enums.PosTerminalStatus;
import com.fdbpay.merchant.service.repository.MerchantRepository;
import com.fdbpay.merchant.service.repository.PosTerminalRepository;
import com.fdbpay.merchant.service.service.PosTerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosTerminalServiceImpl implements PosTerminalService {

    private final PosTerminalRepository posTerminalRepository;
    private final MerchantRepository merchantRepository;

    private static final Duration OFFLINE_THRESHOLD = Duration.ofMinutes(5);

    @Override
    @Transactional
    public PosTerminalResponse register(UUID merchantId, CreatePosTerminalRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        posTerminalRepository.findBySerialNumber(request.getSerialNumber())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                            "Terminal with serial number already exists: " + request.getSerialNumber());
                });

        PosTerminal terminal = PosTerminal.builder()
                .merchantId(merchantId)
                .serialNumber(request.getSerialNumber())
                .type(request.getType())
                .status(PosTerminalStatus.ACTIVE)
                .build();

        terminal = posTerminalRepository.save(terminal);
        log.info("POS terminal registered: terminalId={}, merchantId={}, serialNumber={}",
                terminal.getId(), merchantId, request.getSerialNumber());

        return mapToResponse(terminal);
    }

    @Override
    public List<PosTerminalResponse> getTerminals(UUID merchantId) {
        merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        return posTerminalRepository.findByMerchantId(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public PosTerminalResponse updateStatus(UUID terminalId, PosTerminalStatus status) {
        PosTerminal terminal = posTerminalRepository.findById(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("PosTerminal", terminalId.toString()));

        terminal.setStatus(status);
        terminal = posTerminalRepository.save(terminal);
        log.info("POS terminal status updated: terminalId={}, status={}", terminalId, status);

        return mapToResponse(terminal);
    }

    @Override
    @Transactional
    public PosTerminalResponse heartbeat(UUID terminalId) {
        PosTerminal terminal = posTerminalRepository.findById(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("PosTerminal", terminalId.toString()));

        terminal.setLastPingAt(OffsetDateTime.now());

        if (terminal.getLastPingAt() != null) {
            Duration sinceLastPing = Duration.between(terminal.getLastPingAt(), OffsetDateTime.now());
            if (sinceLastPing.compareTo(OFFLINE_THRESHOLD) > 0 && terminal.getStatus() == PosTerminalStatus.ACTIVE) {
                log.warn("POS terminal was offline for {} minutes: terminalId={}", sinceLastPing.toMinutes(), terminalId);
            }
        }

        if (terminal.getStatus() == PosTerminalStatus.INACTIVE) {
            terminal.setStatus(PosTerminalStatus.ACTIVE);
        }

        terminal = posTerminalRepository.save(terminal);
        log.debug("POS terminal heartbeat: terminalId={}, lastPingAt={}", terminalId, terminal.getLastPingAt());

        return mapToResponse(terminal);
    }

    private PosTerminalResponse mapToResponse(PosTerminal terminal) {
        return PosTerminalResponse.builder()
                .id(terminal.getId())
                .merchantId(terminal.getMerchantId())
                .serialNumber(terminal.getSerialNumber())
                .type(terminal.getType())
                .status(terminal.getStatus())
                .lastPingAt(terminal.getLastPingAt())
                .createdAt(terminal.getCreatedAt())
                .build();
    }
}
