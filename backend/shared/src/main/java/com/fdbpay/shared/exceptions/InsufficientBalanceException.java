package com.fdbpay.shared.exceptions;

import com.fdbpay.shared.constants.ErrorCodes;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(Long available, Long requested) {
        super(ErrorCodes.INSUFFICIENT_BALANCE,
                String.format("Insufficient balance. Available: %d, Requested: %d", available, requested));
    }
}
