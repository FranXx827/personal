package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ResultCode;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(ResultCode.NOT_FOUND, resourceType + "不存在: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(ResultCode.NOT_FOUND, message);
    }
}
