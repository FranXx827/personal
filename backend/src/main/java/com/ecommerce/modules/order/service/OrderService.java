package com.ecommerce.modules.order.service;

import com.ecommerce.common.response.PageResult;
import com.ecommerce.modules.order.dto.OrderCreateRequest;
import com.ecommerce.modules.order.dto.OrderVO;

public interface OrderService {

    OrderVO createOrder(OrderCreateRequest req, Long userId);

    OrderVO getOrderDetail(Long orderId, Long userId);

    OrderVO getOrderByOrderNo(String orderNo, Long userId);

    void cancelOrder(Long orderId, Long userId);

    void payOrder(Long orderId, Long userId);

    void cancelByTimeout(String orderNo);

    PageResult<OrderVO> listUserOrders(Long userId, String status, int page, int pageSize);
}
