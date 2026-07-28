package com.fdbpay.merchant.service.service;

import com.fdbpay.merchant.service.dto.request.CreatePosTerminalRequest;
import com.fdbpay.merchant.service.dto.response.PosTerminalResponse;
import com.fdbpay.merchant.service.model.enums.PosTerminalStatus;

import java.util.List;
import java.util.UUID;

public interface PosTerminalService {

    PosTerminalResponse register(UUID merchantId, CreatePosTerminalRequest request);

    List<PosTerminalResponse> getTerminals(UUID merchantId);

    PosTerminalResponse updateStatus(UUID terminalId, PosTerminalStatus status);

    PosTerminalResponse heartbeat(UUID terminalId);
}
