package com.fdbpay.common.exceptions;

import com.fdbpay.common.constants.ErrorCodes;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String id) {
        super(ErrorCodes.USER_NOT_FOUND, String.format("%s not found with id: %s", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCodes.USER_NOT_FOUND, message);
    }
}
