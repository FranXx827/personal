package com.ecommerce.modules.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.enums.AuditStatus;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.modules.merchant.dto.MerchantApplyRequest;
import com.ecommerce.modules.merchant.dto.MerchantVO;
import com.ecommerce.modules.merchant.entity.Merchant;
import com.ecommerce.modules.merchant.mapper.MerchantMapper;
import com.ecommerce.modules.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    @Override
    public void apply(MerchantApplyRequest req, Long userId) {
        // 注册为商户时可能已建过待审 stub，这里改为 upsert：
        // 已存在则更新资料并重置为待审核，避免「该用户已申请过商户」报错，
        // 从而让 description / contactPhone 等手动录入字段能够真正落库。
        Merchant existing = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, userId)
        );
        if (existing != null) {
            existing.setName(req.name());
            existing.setDescription(req.description());
            existing.setContactPhone(req.contactPhone());
            existing.setAuditStatus(AuditStatus.PENDING.name());
            existing.setRejectReason(null);
            merchantMapper.updateById(existing);
            return;
        }
        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setName(req.name());
        merchant.setDescription(req.description());
        merchant.setContactPhone(req.contactPhone());
        merchant.setAuditStatus(AuditStatus.PENDING.name());
        merchantMapper.insert(merchant);
    }

    @Override
    public List<MerchantVO> getAuditList(String auditStatus) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(Merchant::getAuditStatus, auditStatus);
        }
        return merchantMapper.selectList(wrapper).stream()
                .map(this::toMerchantVO)
                .collect(Collectors.toList());
    }

    @Override
    public MerchantVO approve(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new ResourceNotFoundException("Merchant", merchantId);
        }
        merchant.setAuditStatus(AuditStatus.APPROVED.name());
        merchant.setRejectReason(null);
        merchantMapper.updateById(merchant);
        return toMerchantVO(merchant);
    }

    @Override
    public MerchantVO reject(Long merchantId, String reason) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new ResourceNotFoundException("Merchant", merchantId);
        }
        merchant.setAuditStatus(AuditStatus.REJECTED.name());
        merchant.setRejectReason(reason);
        merchantMapper.updateById(merchant);
        return toMerchantVO(merchant);
    }

    @Override
    public MerchantVO getMyMerchant(Long userId) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, userId)
        );
        if (merchant == null) {
            throw new ResourceNotFoundException("Merchant", userId);
        }
        return toMerchantVO(merchant);
    }

    private MerchantVO toMerchantVO(Merchant merchant) {
        return MerchantVO.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .description(merchant.getDescription())
                .auditStatus(merchant.getAuditStatus())
                .rejectReason(merchant.getRejectReason())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}
