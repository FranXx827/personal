package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ResultCode;

/**
 * 资源冲突异常（如重复创建、库存不足）
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ResultCode.CONFLICT, message);
    }
}
