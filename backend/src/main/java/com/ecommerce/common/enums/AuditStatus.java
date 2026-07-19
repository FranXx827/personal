package com.ecommerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商家审核状态
 */
@Getter
@AllArgsConstructor
public enum AuditStatus {

    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "未通过");

    private final String code;
    private final String desc;

    public static AuditStatus of(String code) {
        for (AuditStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown AuditStatus: " + code);
    }
}
