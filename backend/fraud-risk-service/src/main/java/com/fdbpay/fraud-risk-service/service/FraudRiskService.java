package com.fdbpay.fraud.risk.service.service;

import com.fdbpay.fraud.risk.service.dto.request.SanctionScreeningRequest;
import com.fdbpay.fraud.risk.service.dto.request.TransactionEvaluationRequest;
import com.fdbpay.fraud.risk.service.dto.response.AdminAmlAlertResponse;
import com.fdbpay.fraud.risk.service.dto.response.FraudAlertResponse;
import com.fdbpay.fraud.risk.service.dto.response.FraudEvaluationResponse;
import com.fdbpay.fraud.risk.service.model.enums.AlertSeverity;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface FraudRiskService {

    FraudEvaluationResponse evaluateTransaction(TransactionEvaluationRequest request);

    boolean screenSanctions(SanctionScreeningRequest request);

    Page<FraudAlertResponse> getAlerts(int page, int size);

    FraudAlertResponse resolveAlert(UUID alertId, AlertStatus status);

    List<AdminAmlAlertResponse> getAmlAlerts(AlertSeverity severity, AlertStatus status, int page, int size);

    AdminAmlAlertResponse actionAlert(UUID alertId, String action, String reason);
}
