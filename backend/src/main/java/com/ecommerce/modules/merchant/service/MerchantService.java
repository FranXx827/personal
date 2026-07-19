package com.ecommerce.modules.merchant.service;

import com.ecommerce.modules.merchant.dto.MerchantApplyRequest;
import com.ecommerce.modules.merchant.dto.MerchantVO;

import java.util.List;

public interface MerchantService {

    void apply(MerchantApplyRequest req, Long userId);

    List<MerchantVO> getAuditList(String auditStatus);

    MerchantVO approve(Long merchantId);

    MerchantVO reject(Long merchantId, String reason);

    MerchantVO getMyMerchant(Long userId);
}
