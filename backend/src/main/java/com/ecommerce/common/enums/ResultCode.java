package com.ecommerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务错误码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /* 成功 */
    SUCCESS(0, "ok"),

    /* 通用错误 1xxx */
    PARAM_ERROR(1001, "参数校验失败"),
    NOT_FOUND(1002, "资源不存在"),
    CONFLICT(1003, "资源冲突"),
    TOO_MANY_REQUESTS(1004, "操作过于频繁"),

    /* 鉴权 2xxx */
    UNAUTHORIZED(2001, "未登录 / token 失效"),
    FORBIDDEN(2002, "无权限"),
    ACCOUNT_DISABLED(2003, "账号已被禁用，请联系管理员"),

    /* 商品 3xxx */
    PRODUCT_OFFLINE(3001, "商品已下架"),
    STOCK_INSUFFICIENT(3002, "库存不足"),

    /* 订单 4xxx */
    ORDER_STATUS_INVALID(4001, "订单状态不允许此操作"),
    PAYMENT_FAILED(4002, "支付失败"),

    /* 商家 5xxx */
    MERCHANT_AUDIT_PENDING(5001, "商家入驻审核中"),
    MERCHANT_REJECTED(5002, "商家入驻未通过"),

    /* 系统兜底 */
    SYSTEM_ERROR(99999, "系统异常");

    private final int code;
    private final String message;
}
