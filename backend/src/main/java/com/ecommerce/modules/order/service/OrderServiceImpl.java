package com.ecommerce.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PageResult;
import com.ecommerce.config.RabbitMQConfig;
import com.ecommerce.modules.order.dto.OrderCreateRequest;
import com.ecommerce.modules.order.dto.OrderVO;
import com.ecommerce.modules.order.entity.Order;
import com.ecommerce.modules.order.entity.OrderItem;
import com.ecommerce.modules.order.mapper.OrderItemMapper;
import com.ecommerce.modules.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RabbitTemplate rabbitTemplate;

    private static final Random RANDOM = new Random();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateRequest req, Long userId) {
        // 生成唯一订单号: yyyyMMddHHmmss + 4位随机数
        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", RANDOM.nextInt(10000));

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        // TODO: resolve merchantId from product/sku service
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPayAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setReceiverName(req.receiverName());
        order.setReceiverPhone(req.receiverPhone());
        order.setReceiverAddress(req.receiverAddress());
        order.setRemark(req.remark());
        orderMapper.insert(order);

        // TODO: resolve productTitle, productImage, skuSpecs, price from product/sku service
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setSkuId(req.skuId());
        item.setQuantity(req.quantity() != null ? req.quantity() : 1);
        item.setPrice(BigDecimal.ZERO);
        orderItemMapper.insert(item);

        // 发送延迟消息，超时自动取消
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                orderNo
        );

        log.info("订单创建成功: orderNo={}, userId={}", orderNo, userId);
        return toOrderVO(order, List.of(item));
    }

    @Override
    public OrderVO getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单", orderId);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该订单");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        return toOrderVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单", orderId);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }

        OrderStatus current = OrderStatus.of(order.getStatus());
        OrderStatus target = OrderStateMachine.transition(current, OrderStatus.CANCELED);

        order.setStatus(target.getCode());
        orderMapper.updateById(order);
        log.info("订单已取消: orderId={}, userId={}", orderId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单", orderId);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }

        OrderStatus current = OrderStatus.of(order.getStatus());
        OrderStatus target = OrderStateMachine.transition(current, OrderStatus.PAID);

        order.setStatus(target.getCode());
        orderMapper.updateById(order);
        log.info("订单已支付: orderId={}, userId={}", orderId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByTimeout(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (order == null) {
            log.warn("超时取消订单未找到: orderNo={}", orderNo);
            return;
        }
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.info("订单状态非待支付，跳过超时取消: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        OrderStatus current = OrderStatus.of(order.getStatus());
        OrderStatus target = OrderStateMachine.transition(current, OrderStatus.CANCELED);

        order.setStatus(target.getCode());
        orderMapper.updateById(order);
        log.info("订单超时已自动取消: orderNo={}", orderNo);
    }

    @Override
    public PageResult<OrderVO> listUserOrders(Long userId, String status, int page, int pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }

        IPage<Order> pageResult = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<OrderVO> voList = pageResult.getRecords().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
                    );
                    return toOrderVO(order, items);
                })
                .collect(Collectors.toList());

        return PageResult.of(voList, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    private OrderVO toOrderVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());

        List<OrderVO.OrderItemVO> itemVOs = items.stream()
                .map(item -> new OrderVO.OrderItemVO(
                        item.getSkuId(),
                        item.getProductTitle(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }
}
