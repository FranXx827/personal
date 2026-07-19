package com.ecommerce.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.modules.order.service.OrderService;
import com.ecommerce.modules.payment.dto.PaymentRequest;
import com.ecommerce.modules.payment.dto.PaymentVO;
import com.ecommerce.modules.payment.entity.Payment;
import com.ecommerce.modules.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;

    @Lazy
    private final OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO createPayment(PaymentRequest req, Long userId) {
        var order = orderService.getOrderDetail(req.orderId(), userId);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BusinessException(400, "订单状态不允许支付");
        }

        Payment payment = new Payment();
        payment.setOrderId(req.orderId());
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount());
        payment.setPayMethod(req.payMethod());
        payment.setPayStatus("PENDING");
        paymentMapper.insert(payment);

        log.info("创建支付记录: orderId={}, userId={}, amount={}", req.orderId(), userId, order.getTotalAmount());
        return toPaymentVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO handlePaymentCallback(String transactionId, String payStatus, Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId)
                        .orderByDesc(Payment::getCreatedAt).last("LIMIT 1")
        );
        if (payment == null) {
            throw new ResourceNotFoundException("支付记录", orderId);
        }

        payment.setTransactionId(transactionId);
        payment.setPayStatus(payStatus);
        paymentMapper.updateById(payment);

        if ("SUCCESS".equals(payStatus)) {
            orderService.payOrder(orderId, payment.getUserId());
            log.info("支付回调成功: orderId={}, transactionId={}", orderId, transactionId);
        }

        return toPaymentVO(payment);
    }

    @Override
    public PaymentVO getPaymentByOrderId(Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId)
                        .orderByDesc(Payment::getCreatedAt).last("LIMIT 1")
        );
        if (payment == null) {
            throw new ResourceNotFoundException("支付记录", orderId);
        }
        return toPaymentVO(payment);
    }

    private PaymentVO toPaymentVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setOrderId(payment.getOrderId());
        vo.setAmount(payment.getAmount());
        vo.setPayStatus(payment.getPayStatus());
        return vo;
    }
}
