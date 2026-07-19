package com.ecommerce.modules.payment.service;

import com.ecommerce.modules.payment.dto.PaymentRequest;
import com.ecommerce.modules.payment.dto.PaymentVO;

public interface PaymentService {

    PaymentVO createPayment(PaymentRequest req, Long userId);

    PaymentVO handlePaymentCallback(String transactionId, String payStatus, Long orderId);

    PaymentVO getPaymentByOrderId(Long orderId);
}
