package com.fdbpay.shared.exceptions;

import com.fdbpay.shared.constants.ErrorCodes;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, String id) {
        super(ErrorCodes.USER_NOT_FOUND, String.format("%s not found with id: %s", resource, id));
    }
}
