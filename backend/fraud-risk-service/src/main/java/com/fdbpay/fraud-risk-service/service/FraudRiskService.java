package com.fdbpay.fraud.risk.service.service;

import com.fdbpay.fraud.risk.service.dto.request.SanctionScreeningRequest;
import com.fdbpay.fraud.risk.service.dto.request.TransactionEvaluationRequest;
import com.fdbpay.fraud.risk.service.dto.response.FraudAlertResponse;
import com.fdbpay.fraud.risk.service.dto.response.FraudEvaluationResponse;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import org.springframework.data.domain.Page;

public interface FraudRiskService {

    FraudEvaluationResponse evaluateTransaction(TransactionEvaluationRequest request);

    boolean screenSanctions(SanctionScreeningRequest request);

    Page<FraudAlertResponse> getAlerts(int page, int size);

    FraudAlertResponse resolveAlert(java.util.UUID alertId, AlertStatus status);
}
