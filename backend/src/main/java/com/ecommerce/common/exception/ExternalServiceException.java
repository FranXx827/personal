package com.ecommerce.common.exception;

public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(String serviceName, String method, Throwable cause) {
        super("外部服务[" + serviceName + "]调用[" + method + "]失败: " + cause.getMessage(), cause);
    }
}
