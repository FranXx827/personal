package com.ecommerce.common.exception;

import com.ecommerce.common.enums.ResultCode;

public class StateMachineException extends BusinessException {

    public StateMachineException(String message) {
        super(ResultCode.ORDER_STATUS_INVALID, message);
    }

    public StateMachineException(String currentStatus, String targetAction) {
        super(ResultCode.ORDER_STATUS_INVALID,
                "当前状态[" + currentStatus + "]不允许执行[" + targetAction + "]操作");
    }
}
